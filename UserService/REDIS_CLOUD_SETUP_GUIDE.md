# Redis Cloud Setup Guide (Free Tier)

This guide will walk you through setting up Redis Cloud (free tier) for your application.

## Why Redis Cloud?

- ✅ **Free tier available** (30MB storage)
- ✅ **Publicly accessible** - No VPC configuration needed
- ✅ **Easy setup** - Get started in minutes
- ✅ **Perfect for development** - No AWS complexity
- ✅ **SSL/TLS support** included

---

## Step-by-Step Setup

### Step 1: Sign Up for Redis Cloud

1. Go to **https://redis.com/try-free/** or **https://redis.com/cloud/**
2. Click **"Try Free"** or **"Get Started"**
3. Sign up with:
   - Email address
   - Password
   - Or use Google/GitHub sign-in
4. Verify your email if required

### Step 2: Create a Free Database

1. After logging in, you'll see the **Redis Cloud dashboard**
2. Click **"New Subscription"** or **"Create Subscription"**
3. Choose **"Fixed"** plan (free tier)
4. Select **"Free"** tier (30MB)
5. Choose your **cloud provider** and **region**:
   - AWS, GCP, or Azure
   - Select region closest to you (e.g., `us-east-1`)
6. Click **"Create Subscription"**

### Step 3: Create a Redis Database

1. In your subscription, click **"New Database"** or **"+"** button
2. **Database configuration:**
   - **Name**: Give it a name (e.g., `sparta-redis`)
   - **Type**: Redis (default)
   - **Memory limit**: 30MB (free tier)
   - **Replication**: Disabled (for free tier)
   - **Data persistence**: Optional (uses more memory)
3. Click **"Create Database"**
4. Wait 1-2 minutes for database to be created

### Step 4: Get Connection Details

1. Once database is created, click on your database name
2. You'll see the **database details page**
3. Look for **"Endpoint"** or **"Public endpoint"** section:
   - **Host**: Something like `redis-12345.c123.us-east-1-1.ec2.cloud.redislabs.com`
   - **Port**: Usually `12345` or similar (NOT 6379)
   - **SSL Port**: Usually `12346` or similar (for SSL connections)

4. Look for **"Access Control & Security"** or **"Security"** section:
   - **Default user password**: This is your Redis password
   - Or you may see **"Access Control List"** with username/password

5. **Copy these values:**
   - Host/Endpoint
   - Port (use SSL port if you want SSL)
   - Password

### Step 5: Test Connection (Optional)

You can test the connection using Redis CLI:

```bash
# Install Redis CLI (if not installed)
# Windows: Download from https://github.com/microsoftarchive/redis/releases
# Mac: brew install redis
# Linux: sudo apt-get install redis-tools

# Connect (replace with your actual values)
redis-cli -h your-host.redis.cloud -p 12345 -a your-password

# Test
PING
# Should return: PONG
```

---

## Step 6: Update Application Configuration

Update your `application.properties` file:

```properties
# Redis Cloud Configuration (Free Tier)
spring.data.redis.host=your-host.redis.cloud
spring.data.redis.port=12345
spring.data.redis.password=your-redis-cloud-password
spring.data.redis.ssl=true
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
```

**Important Notes:**
- Use the **SSL port** if you set `spring.data.redis.ssl=true`
- Use the **regular port** if you set `spring.data.redis.ssl=false`
- The port is usually **NOT 6379** (it's a custom port assigned by Redis Cloud)

---

## Example Configuration

After setup, your configuration might look like:

```properties
# Redis Cloud Configuration
spring.data.redis.host=redis-12345.c123.us-east-1-1.ec2.cloud.redislabs.com
spring.data.redis.port=12345
spring.data.redis.password=MySecurePassword123!
spring.data.redis.ssl=true
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
```

---

## Troubleshooting

### Connection Timeout

**Problem**: Cannot connect to Redis Cloud

**Solutions**:
1. Verify host and port are correct
2. Check if database is in "Active" status
3. Verify password is correct
4. Check if you're using SSL port with `ssl=true`
5. Try disabling SSL: `spring.data.redis.ssl=false` (use regular port)

### Authentication Failed

**Problem**: "NOAUTH Authentication required" or "WRONGPASS"

**Solutions**:
1. Verify password matches the one in Redis Cloud dashboard
2. Check if you need a username (some Redis Cloud instances require username)
3. If username required, you may need to use format: `username:password`

### SSL Handshake Error

**Problem**: SSL connection fails

**Solutions**:
1. If using SSL port, ensure `spring.data.redis.ssl=true`
2. If using regular port, set `spring.data.redis.ssl=false`
3. Try switching between SSL and non-SSL ports

---

## Free Tier Limitations

- **30MB storage** - Suitable for development/testing
- **No replication** - Single node only
- **Limited throughput** - May have rate limits
- **No persistence** (optional, uses more memory)

**For production**, consider upgrading to a paid plan.

---

## Security Best Practices

1. **Use strong passwords** - Don't use default passwords
2. **Enable SSL** - Always use SSL in production
3. **Don't commit passwords** - Use environment variables:
   ```properties
   spring.data.redis.password=${REDIS_PASSWORD}
   ```
4. **Monitor usage** - Check Redis Cloud dashboard regularly
5. **Backup data** - Export important data regularly

---

## Alternative: Upstash Redis (Another Free Option)

If Redis Cloud doesn't work for you, try **Upstash Redis**:

1. Go to **https://upstash.com/**
2. Sign up for free account
3. Create a Redis database
4. Get connection details (similar process)
5. Free tier: 10,000 commands/day

---

## Need Help?

- Redis Cloud Documentation: https://docs.redis.com/
- Redis Cloud Support: https://redis.com/support/
- Redis Cloud Status: https://status.redis.com/

