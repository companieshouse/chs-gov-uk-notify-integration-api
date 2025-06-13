# GOV.UK Notify Mock Implementation

This package provides a transparent mock implementation of the GOV.UK Notify service that can be used for development
and testing purposes.

## Features

- Automatically activates only in non-production environments (`local`, `dev`, `test` profiles)
- Can be toggled on/off via reference prefixes without changing any application code
- Seamlessly falls back to the real implementation when references don't match the pattern
- Simulates rate limiting with configurable limits
- Provides realistic mock responses for emails and letters
- Can simulate random failures at configurable rates

## How It Works

The mock implementation uses Spring's profile-based autowiring to automatically substitute the real NotificationClient
with a mock version in non-production environments. The mock version checks each reference to determine whether to use
mock behavior:

- When a reference starts with `use-mock-notify`, mock behaviors are used
- When a reference doesn't match the pattern, calls are delegated to the real NotificationClient

## Configuration

The mock is configured through application properties:

```properties
# Mock configuration for non-production environments
# These will only be used when active profile is local, dev, or test
# and when the reference starts with "use-mock-notify"
notify.mock.minute-limit=3000
notify.mock.daily-limit=250000
notify.mock.simulate-failures=false
notify.mock.failure-rate=0.1
```

## Rate Limiting and Failure Simulation

The mock provides realistic rate limiting simulation with both per-minute and daily limits:

Based on the limits defined here: https://docs.notifications.service.gov.uk/java.html#limits

1. **Per-minute rate limiting**:
    - Default: 3000 requests/minute
    - When exceeded: Returns HTTP 429 error
    - Automatically resets after one minute has passed

2. **Daily rate limiting**:
    - Default: 250,000 requests/day
    - When exceeded: Returns HTTP 429 error
    - Resets at the start of a new day

3. **Failure simulation**:
    - Can be configured to randomly fail requests
    - Configurable failure rate (0.0 to 1.0)
    - Returns HTTP 500 errors when triggered

## Safety Features

- Mock is only activated in non-production environments through Spring profiles
- Production environments always use the real implementation regardless of reference patterns
- The mock client proxies to the real implementation when references don't match the pattern
