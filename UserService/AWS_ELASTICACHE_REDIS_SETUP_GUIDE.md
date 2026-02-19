# AWS ElastiCache Redis Setup Guide

This guide will walk you through setting up AWS ElastiCache Redis, getting the password, and configuring it for public access.

## Prerequisites
- An AWS account with access to ElastiCache
- Understanding of AWS VPC and Security Groups

---

## Part 1: Creating ElastiCache Redis Cluster

### Step 1: Access AWS ElastiCache Console

1. Log in to your AWS account: https://console.aws.amazon.com/
2. In the search bar, type **"ElastiCache"** and select it from the services dropdown
3. You'll be taken to the ElastiCache Dashboard

### Step 2: Create Redis Cluster

1. Click **"Create cluster"** or **"Redis cluster"** button
2. Choose **"Redis"** as the engine type
3. **Cluster settings:**
   - **Name**: Give it a unique name (e.g., `sparta-redis-cluster`)
   - **Description**: Optional description
   - **Engine version**: Choose latest stable version (recommended: 7.x or 6.x)
   - **Port**: Default is `6379` (keep this)
   - **Node type**: Choose instance size (e.g., `cache.t3.micro` for free tier testing)
   - **Number of replicas**: `0` for single node (or `1` for high availability)

4. **Subnet group**: 
   - Create new or select existing subnet group
   - **IMPORTANT**: For public access, you need subnets with internet gateway

5. **Security**: 
   - **Encryption in-transit**: Enable this (required for SSL)
   - **Encryption at-rest**: Optional but recommended
   - **Auth token**: **ENABLE THIS** - This is your Redis password!
     - Click **"Create new"** or **"Select existing"**
     - Enter a strong password (save this - you'll need it!)
     - This password will be used in `spring.data.redis.password`

6. **Availability Zone**: Choose your preferred zone

7. **Maintenance**: Configure maintenance window (optional)

8. Click **"Create cluster"**
9. Wait 5-15 minutes for the cluster to be created
10. Status will change to **"Available"** when ready

---

## Part 2: Getting Redis Connection Details

### Step 1: Find Your Redis Endpoint

1. In ElastiCache Console, click on **"Redis clusters"** in the left sidebar
2. Click on your cluster name (e.g., `sparta-redis-cluster`)
3. You'll see the cluster details page
4. Look for **"Primary endpoint"** section:
   - **Endpoint**: Something like `sparta-redis-cluster.abc123.0001.use1.cache.amazonaws.com`
   - **Port**: `6379`
5. **Copy the endpoint** - this goes in `spring.data.redis.host`

### Step 2: Get Your Redis Password (Auth Token)

**If you enabled Auth Token during creation:**

1. On the cluster details page, scroll to **"Security"** section
2. Look for **"AUTH token"** field
3. You'll see either:
   - The token value (if you just created it)
   - Or a button to **"View token"** or **"Show token"**
4. **Click to reveal the token** - this is your Redis password
5. **Copy and save it securely** - this goes in `spring.data.redis.password`

**If you forgot the password or need to reset it:**

1. Go to **AWS Secrets Manager** (if you stored it there)
2. Or go to ElastiCache → Your cluster → **"Modify"** → **"Security"** → **"Change auth token"**
3. Create a new auth token and save it

**Note**: If you didn't enable auth token, you can leave `spring.data.redis.password` empty, but this is **NOT RECOMMENDED** for security.

---

## Part 3: Making Redis Publicly Accessible

**⚠️ SECURITY WARNING**: Making Redis publicly accessible is a security risk. Only do this for development/testing. For production, use VPC peering or VPN.

### Step 1: Configure VPC and Subnets

1. Go to **VPC Console** → **Subnets**
2. Find the subnets used by your ElastiCache cluster
3. Check if they have an **Internet Gateway** attached:
   - Go to **VPC Console** → **Internet Gateways**
   - If no IGW exists, create one and attach to your VPC
   - Go to **Route Tables** → Edit routes → Add route:
     - Destination: `0.0.0.0/0`
     - Target: Your Internet Gateway

### Step 2: Modify ElastiCache Cluster for Public Access

**IMPORTANT**: ElastiCache Redis clusters are **NOT directly publicly accessible** by default. You have two options:

#### Option A: Use EC2 Instance as Proxy (Recommended for Development)

1. Create an EC2 instance in the same VPC as your ElastiCache cluster
2. Install Redis on EC2:
   ```bash
   sudo yum install redis -y
   # or
   sudo apt-get install redis-server -y
   ```
3. Configure Redis on EC2 to forward to ElastiCache:
   - Edit `/etc/redis/redis.conf`
   - Set up Redis replication or proxy
4. Configure EC2 security group to allow port 6379 from your IP
5. Connect to EC2's public IP instead of ElastiCache endpoint

#### Option B: Use AWS VPN or Direct Connect (Production)

For production, use:
- **AWS VPN**: Connect your on-premises network to VPC
- **AWS Direct Connect**: Dedicated network connection
- **VPC Peering**: If connecting from another AWS account/VPC

### Step 3: Configure Security Group

1. Go to **EC2 Console** → **Security Groups**
2. Find the security group attached to your ElastiCache cluster
3. Click **"Edit inbound rules"**
4. Add rule:
   - **Type**: Custom TCP
   - **Port**: `6379`
   - **Source**: 
     - For testing: `0.0.0.0/0` (allows from anywhere - **NOT SECURE**)
     - For production: Your specific IP address (e.g., `123.45.67.89/32`)
   - **Description**: "Allow Redis from application"
5. Click **"Save rules"**

**Note**: ElastiCache clusters in a VPC use VPC security groups, not ElastiCache security groups.

### Step 4: Update Application Properties

Update your `application.properties`:

```properties
# AWS ElastiCache Redis Configuration
spring.data.redis.host=your-actual-endpoint.cache.amazonaws.com
spring.data.redis.port=6379
spring.data.redis.password=your-actual-auth-token-password
spring.data.redis.ssl=true
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
```

**If using EC2 proxy** (Option A):
```properties
spring.data.redis.host=your-ec2-public-ip-or-dns
spring.data.redis.port=6379
spring.data.redis.password=your-actual-auth-token-password
spring.data.redis.ssl=false  # Usually false for EC2 proxy
```

---

## Part 4: Alternative - Using Redis Cloud or Local Redis for Development

If you need public access for development, consider these alternatives:

### Option 1: Redis Cloud (Free Tier Available)

1. Sign up at https://redis.com/try-free/
2. Create a free database
3. Get connection details from dashboard
4. Update `application.properties`:
```properties
spring.data.redis.host=your-redis-cloud-endpoint.redis.cloud
spring.data.redis.port=12345
spring.data.redis.password=your-redis-cloud-password
spring.data.redis.ssl=true
```

### Option 2: Local Redis (For Local Development)

1. Install Redis locally:
   ```bash
   # Windows (using WSL or Docker)
   docker run -d -p 6379:6379 redis:latest
   
   # Mac
   brew install redis
   redis-server
   
   # Linux
   sudo apt-get install redis-server
   sudo systemctl start redis
   ```

2. Update `application.properties`:
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=  # Leave empty for local Redis without auth
spring.data.redis.ssl=false
```

---

## Security Best Practices

1. **Never commit passwords to Git**: Use environment variables
   ```properties
   spring.data.redis.password=${REDIS_PASSWORD}
   ```

2. **Use AWS Secrets Manager**:
   - Store Redis password in AWS Secrets Manager
   - Retrieve in application code using AWS SDK

3. **Restrict Security Groups**:
   - Only allow connections from your application's IP
   - Never use `0.0.0.0/0` in production

4. **Enable Encryption**:
   - Always enable encryption in-transit (SSL/TLS)
   - Enable encryption at-rest for sensitive data

5. **Use VPC for Production**:
   - Keep ElastiCache in private subnets
   - Access via VPN or VPC peering only

---

## Troubleshooting

### Connection Timeout

**Problem**: Cannot connect to Redis

**Solutions**:
1. Check security group allows port 6379 from your IP
2. Verify VPC route tables have internet gateway route
3. Check if ElastiCache cluster is in "Available" status
4. Verify endpoint and port are correct
5. Check if auth token/password is correct

### Authentication Failed

**Problem**: "NOAUTH Authentication required" or "WRONGPASS"

**Solutions**:
1. Verify `spring.data.redis.password` matches the auth token
2. Check if auth token is enabled on cluster
3. Try resetting auth token in ElastiCache console

### SSL Connection Error

**Problem**: SSL handshake fails

**Solutions**:
1. Verify `spring.data.redis.ssl=true` matches cluster encryption setting
2. If cluster doesn't have encryption, set `spring.data.redis.ssl=false`
3. Check if you're using correct endpoint (primary endpoint, not reader endpoint)

---

## Quick Reference: Finding Values in AWS Console

| Value | Where to Find |
|-------|---------------|
| **Endpoint** | ElastiCache → Redis clusters → Your cluster → Primary endpoint |
| **Port** | ElastiCache → Redis clusters → Your cluster → Port (usually 6379) |
| **Password** | ElastiCache → Redis clusters → Your cluster → Security → AUTH token |
| **Security Group** | ElastiCache → Redis clusters → Your cluster → Security → Security groups |

---

## Example Configuration

After setup, your `application.properties` should look like:

```properties
# AWS ElastiCache Redis Configuration
spring.data.redis.host=sparta-redis-cluster.abc123.0001.use1.cache.amazonaws.com
spring.data.redis.port=6379
spring.data.redis.password=MySecureRedisPassword123!
spring.data.redis.ssl=true
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
```

---

## Need Help?

- AWS ElastiCache Documentation: https://docs.aws.amazon.com/elasticache/
- Redis Documentation: https://redis.io/documentation
- AWS Support: https://aws.amazon.com/support/

