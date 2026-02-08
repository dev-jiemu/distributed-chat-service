#!/usr/bin/env python3
import asyncio
import httpx
import time
import random
import string

# Configuration
BASE_URL = "http://localhost:8081/api/auth"
REGISTER_URL = f"{BASE_URL}/register"
NUM_REQUESTS = 100  # Total number of registration attempts
CONCURRENCY = 20    # Number of concurrent workers

async def register_user(client, email, password, confirm_password, current_user_id):
    """Attempts to register a new user."""
    payload = {
        "email": email,
        "password": password,
        "confirmPassword": confirm_password,
        "currentUserId": current_user_id
    }
    try:
        # Give more time for the server to respond, especially under load
        response = await client.post(REGISTER_URL, json=payload, timeout=20.0)
        if response.status_code == 201:
            return "success"
        elif response.status_code == 409:
            return "conflict" # Already registered
        else:
            return f"error_{response.status_code}"
    except httpx.ReadTimeout:
        return "timeout"
    except Exception:
        return "exception"

def generate_random_string(length=8):
    """Generates a random string for username/password."""
    letters = string.ascii_lowercase
    return ''.join(random.choice(letters) for i in range(length))

async def main():
    """Main function to run the load test."""
    print("="*60)
    print("🚀 Auth Service Load Test")
    print("="*60)
    print(f"URL: {REGISTER_URL}")
    print(f"Total Requests: {NUM_REQUESTS}")
    print(f"Concurrency Level: {CONCURRENCY}")
    print("-" * 60)

    success_count = 0
    conflict_count = 0
    timeout_count = 0
    error_counts = {}

    start_time = time.time()

    async with httpx.AsyncClient() as client:
        semaphore = asyncio.Semaphore(CONCURRENCY)
        tasks = []

        # Use a set to ensure emails and user IDs are unique
        generated_emails = set()
        while len(generated_emails) < NUM_REQUESTS:
            # Generate unique email and user_id for each request
            email = f"testuser_{int(time.time() * 1000)}_{len(generated_emails)}@example.com"
            generated_emails.add(email)

        emails_to_test = list(generated_emails)

        async def task_wrapper(email):
            async with semaphore:
                password = generate_random_string()
                confirm_password = password
                current_user_id = generate_random_string(16) # Generate a random user ID
                return await register_user(client, email, password, confirm_password, current_user_id)

        for email in emails_to_test:
            tasks.append(asyncio.create_task(task_wrapper(email)))

        results = await asyncio.gather(*tasks)

    end_time = time.time()
    duration = end_time - start_time

    for result in results:
        if result == "success":
            success_count += 1
        elif result == "conflict":
            conflict_count += 1
        elif result == "timeout":
            timeout_count += 1
        else:
            error_counts[result] = error_counts.get(result, 0) + 1

    print("\n📊 Test Results:")
    print("-" * 60)
    print(f"Total time taken: {duration:.2f} seconds")
    if duration > 0:
        print(f"Requests per second (RPS): {NUM_REQUESTS / duration:.2f}")
    print(f"✅ Successful registrations: {success_count}")
    print(f"⚠️ Conflicts (already registered): {conflict_count}")
    print(f"⏳ Timeouts: {timeout_count}")

    if error_counts:
        print("❌ Errors:")
        for code, count in error_counts.items():
            print(f"  - {code}: {count}")

    print("="*60)

    if timeout_count > 50: # If more than half the requests time out
        print("\n🔥 High number of timeouts detected!")
        print("This strongly indicates a severe concurrency bottleneck on the server.")
        print("The database connection pool is likely too small or the DB cannot handle the load.")
    elif duration > (NUM_REQUESTS / CONCURRENCY) * 0.8: # A rough heuristic for slowness
         print("\n🤔 Performance seems slow.")
         print("While not timing out, the server is taking a long time to process requests.")
         print("This could still point to a bottleneck.")
    else:
        print("\n👍 Performance seems acceptable.")


if __name__ == "__main__":
    asyncio.run(main())
