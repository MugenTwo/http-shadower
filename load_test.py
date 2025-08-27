#!/usr/bin/env python3
"""
Load testing script for HTTP Shadower application.
Run this after start-all.sh has been executed.
"""

import requests
import time
import threading
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from typing import List
import json
import random

@dataclass
class TestResult:
    success: bool
    response_time: float
    status_code: int
    error: str = None

class LoadTester:
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.session = requests.Session()
        
    def make_request(self, endpoint: str = "/", method: str = "GET", data: dict = None) -> TestResult:
        """Make a single HTTP request and return timing/result info"""
        start_time = time.time()
        try:
            url = f"{self.base_url}{endpoint}"
            
            if method.upper() == "GET":
                response = self.session.get(url, timeout=30)
            elif method.upper() == "POST":
                response = self.session.post(url, json=data or {}, timeout=30)
            elif method.upper() == "PUT":
                response = self.session.put(url, json=data or {}, timeout=30)
            else:
                response = self.session.get(url, timeout=30)
                
            response_time = time.time() - start_time
            return TestResult(
                success=True,
                response_time=response_time,
                status_code=response.status_code
            )
        except Exception as e:
            response_time = time.time() - start_time
            return TestResult(
                success=False,
                response_time=response_time,
                status_code=0,
                error=str(e)
            )

    def run_load_test(self, 
                     num_requests: int = 100, 
                     concurrent_users: int = 10,
                     test_endpoints: List[str] = None) -> tuple[List[TestResult], float]:
        """Run load test with specified parameters"""
        
        if test_endpoints is None:
            test_endpoints = [
                "/",
                "/api/health",
                "/api/status",
                "/test/endpoint"
            ]
        
        print(f"Starting load test with {num_requests} requests using {concurrent_users} concurrent users")
        print(f"Target: {self.base_url}")
        print(f"Test endpoints: {test_endpoints}")
        print("-" * 60)
        
        results = []
        start_time = time.time()
        
        with ThreadPoolExecutor(max_workers=concurrent_users) as executor:
            futures = []
            
            for i in range(num_requests):
                endpoint = random.choice(test_endpoints)
                method = random.choice(["GET", "POST"] if endpoint != "/" else ["GET"])
                data = {"test": True, "request_id": i} if method == "POST" else None
                
                future = executor.submit(self.make_request, endpoint, method, data)
                futures.append(future)
            
            for future in as_completed(futures):
                result = future.result()
                results.append(result)
                
                if len(results) % 50 == 0:
                    print(f"Completed {len(results)}/{num_requests} requests...")
        
        total_time = time.time() - start_time
        return results, total_time

    def print_stats(self, results: List[TestResult], total_test_time: float):
        """Print comprehensive test statistics"""
        if not results:
            print("No results to analyze")
            return
        
        successful_results = [r for r in results if r.success]
        failed_results = [r for r in results if not r.success]
        
        total_requests = len(results)
        success_rate = (len(successful_results) / total_requests) * 100
        
        if successful_results:
            response_times = [r.response_time for r in successful_results]
            avg_response_time = sum(response_times) / len(response_times)
            min_response_time = min(response_times)
            max_response_time = max(response_times)
            
            # Calculate percentiles
            sorted_times = sorted(response_times)
            p50 = sorted_times[len(sorted_times) // 2]
            p95 = sorted_times[int(len(sorted_times) * 0.95)]
            p99 = sorted_times[int(len(sorted_times) * 0.99)]
        else:
            avg_response_time = min_response_time = max_response_time = 0
            p50 = p95 = p99 = 0
        
        # Status code distribution
        status_codes = {}
        for result in successful_results:
            status_codes[result.status_code] = status_codes.get(result.status_code, 0) + 1
        
        print("\n" + "=" * 60)
        print("LOAD TEST RESULTS")
        print("=" * 60)
        print(f"Total Requests: {total_requests}")
        print(f"Successful: {len(successful_results)} ({success_rate:.1f}%)")
        print(f"Failed: {len(failed_results)} ({100-success_rate:.1f}%)")
        print()
        
        if successful_results:
            print("RESPONSE TIME STATISTICS:")
            print(f"  Average: {avg_response_time:.3f}s")
            print(f"  Min: {min_response_time:.3f}s")
            print(f"  Max: {max_response_time:.3f}s")
            print(f"  50th percentile: {p50:.3f}s")
            print(f"  95th percentile: {p95:.3f}s")
            print(f"  99th percentile: {p99:.3f}s")
            print()
            
            print("STATUS CODE DISTRIBUTION:")
            for code, count in sorted(status_codes.items()):
                print(f"  {code}: {count} requests")
            print()
        
        if failed_results:
            print("ERRORS:")
            error_types = {}
            for result in failed_results:
                error_type = result.error.split(':')[0] if result.error else "Unknown"
                error_types[error_type] = error_types.get(error_type, 0) + 1
            
            for error, count in error_types.items():
                print(f"  {error}: {count} occurrences")
            print()
        
        # Throughput calculation
        if successful_results and total_test_time > 0:
            throughput = len(successful_results) / total_test_time
            print(f"THROUGHPUT: {throughput:.2f} requests/second")
            print(f"TOTAL TEST TIME: {total_test_time:.2f} seconds")
        
        print("=" * 60)

def wait_for_service(base_url: str, max_wait: int = 30) -> bool:
    """Wait for the service to be available"""
    print(f"Waiting for service at {base_url} to be ready...")
    
    for i in range(max_wait):
        try:
            response = requests.get(f"{base_url}/", timeout=5)
            if response.status_code < 500:
                print(f"Service is ready! (responded with status {response.status_code})")
                return True
        except requests.exceptions.RequestException:
            pass
        
        if i < max_wait - 1:
            print(f"Service not ready, waiting... ({i+1}/{max_wait})")
            time.sleep(1)
    
    print(f"Service at {base_url} did not become ready within {max_wait} seconds")
    return False

def main():
    parser = argparse.ArgumentParser(description="Load test HTTP Shadower application")
    parser.add_argument("--url", default="http://localhost:8080", 
                       help="Base URL of the application (default: http://localhost:8080)")
    parser.add_argument("--requests", type=int, default=100,
                       help="Number of requests to send (default: 100)")
    parser.add_argument("--concurrent", type=int, default=10,
                       help="Number of concurrent users (default: 10)")
    parser.add_argument("--wait", action="store_true",
                       help="Wait for service to be ready before starting test")
    parser.add_argument("--endpoints", nargs="+", 
                       default=["/", "/health", "/api/test", "/echo"],
                       help="Endpoints to test (default: / /health /api/test /echo)")
    
    args = parser.parse_args()
    
    if args.wait:
        if not wait_for_service(args.url):
            print("Aborting test - service not ready")
            return 1
    
    tester = LoadTester(args.url)
    
    results, total_test_time = tester.run_load_test(
        num_requests=args.requests,
        concurrent_users=args.concurrent,
        test_endpoints=args.endpoints
    )
    
    print(f"\nTest completed in {total_test_time:.2f} seconds")
    tester.print_stats(results, total_test_time)
    
    return 0 if all(r.success for r in results) else 1

if __name__ == "__main__":
    exit(main())