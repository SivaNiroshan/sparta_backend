# AWS RDS PostgreSQL Connection Setup Guide

This guide will walk you through finding all the connection values needed for your `application.properties` file.

## Prerequisites
- An AWS account with access to RDS
- An existing RDS PostgreSQL instance (or create one if you don't have it)

---

## Step-by-Step Instructions

### Step 1: Access AWS RDS Console

1. Log in to your AWS account: https://console.aws.amazon.com/
2. In the search bar at the top, type **"RDS"** and select **"RDS"** from the services dropdown
3. You'll be taken to the RDS Dashboard

### Step 2: Find Your PostgreSQL Database Instance

1. In the left sidebar, click on **"Databases"**
2. You'll see a list of all your RDS database instances
3. Click on the **PostgreSQL database instance** you want to connect to
   - If you don't have one, see "Creating a New RDS Instance" section below

### Step 3: Get the Database Endpoint (Connection URL)

**IMPORTANT: You must click on the database name to see the detail page!**

**Detailed Steps:**

1. **On the Databases list page** (where you see "database-1" in the table):
   - **Click directly on the database name "database-1"** (the blue highlighted text/row)
   - This will open the **database detail page** (NOT the list page)

2. **Once on the detail page**, you'll see:
   - The database name at the top (e.g., "database-1")
   - **Tabs at the top** of the page: **"Monitoring"**, **"Configuration"**, **"Connectivity & security"**, **"Logs & events"**, etc.
   - These tabs are ONLY visible on the detail page, NOT on the list page

3. **Click on the "Connectivity & security" tab** (it's usually the second or third tab from the left)

4. **Scroll down** to find the **"Endpoint & port"** section

5. You'll see:
   - **Endpoint**: This is a long URL like `database-1.abc123xyz.us-east-1.rds.amazonaws.com`
   - **Port**: Usually `5432` for PostgreSQL
   - A **"Copy" button** next to the endpoint

6. **Click the "Copy" button** next to the endpoint, or manually copy the entire endpoint

7. This endpoint is your `your-rds-endpoint.region.rds.amazonaws.com`

**Visual Guide:**
- **List Page** (where you are now): Shows table with columns like "DB identifier", "Status", "Engine"
- **Detail Page** (where you need to go): Shows tabs at the top and detailed information below
- **To get to detail page**: Click on "database-1" text/row in the table

**Visual Guide:**
- Look for a section that says **"Endpoint & port"** or **"Endpoint"**
- The endpoint will be in a text box with a copy button
- It typically looks like: `your-db-name.xxxxxxxxxx.region.rds.amazonaws.com`

**Example:**
- Endpoint: `database-1.abc123xyz.us-east-1.rds.amazonaws.com`
- Port: `5432`
- Your connection URL will be: `jdbc:postgresql://database-1.abc123xyz.us-east-1.rds.amazonaws.com:5432`

**If you still can't find it:**
- Try clicking on different tabs: "Connectivity & security", "Configuration", or "Monitoring"
- The endpoint might also be visible in the main database overview page (right side panel)
- Look for any field labeled "Endpoint", "DB endpoint", or "Connection string"

### Step 4: Get the Database Name

**Detailed Steps:**

1. **Make sure you're on the database detail page** (not the list page)
   - You should see tabs at the top: "Monitoring", "Configuration", "Connectivity & security", etc.
   - If you don't see tabs, click on "database-1" from the Databases list first

2. **Click on the "Configuration" tab** (usually the first or second tab from the left)

3. **Scroll down** through the configuration details

4. Look for a field labeled:
   - **"DB name"**
   - **"Database name"**
   - **"Initial database name"**
   - It's usually in the "Database" or "Instance details" section

5. **Copy this value** - this is your `your-database-name`

**Common Database Names:**
- If you specified a name during creation, use that
- If you left it blank or used default: usually `postgres`
- Other common defaults: `mydb`, `database`, or the instance identifier name

**If you can't find "DB name" in Configuration:**
- Check the **"Connectivity & security"** tab - sometimes it's shown there
- Look at the **main database overview** (the page you see when you first click the database)
- **Alternative**: If you remember what you entered during database creation, use that
- **Last resort**: Try `postgres` (the default PostgreSQL database name)

**Note:** If you didn't specify a database name during creation, AWS might have created one automatically. Check your creation logs or try connecting with `postgres` as the database name.

### Step 5: Get the Master Username

1. On the same database instance page, check the **"Configuration"** tab
2. Look for **"Master username"** or **"DB username"**
3. **Copy this value** - this is your `your-username`
   - Common default: `postgres` or a custom username you specified

**Note:** This is the username you set when creating the RDS instance.

### Step 6: Get or Reset the Master Password

**If you remember your password:**
- Use the password you set when creating the RDS instance

**If you forgot your password:**
1. On the database instance page, click **"Modify"** button (top right)
2. Scroll down to **"Credentials & authentication"** section
3. Check **"Change master password"**
4. Enter a new password (must be 8-128 characters)
5. Click **"Continue"** at the bottom
6. Choose when to apply changes:
   - **"Apply immediately"** - applies right away (may cause brief downtime)
   - **"During the next maintenance window"** - applies during scheduled maintenance
7. Click **"Modify DB instance"**
8. **Save this password securely** - this is your `your-password`

**Important:** After changing the password, wait a few minutes for the change to take effect.

---

## Updating Your application.properties File

Once you have all the values, update your `application.properties` file:

```properties
# AWS RDS PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://YOUR_ENDPOINT:5432/YOUR_DATABASE_NAME
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver
```

### Example:
```properties
spring.datasource.url=jdbc:postgresql://mydbinstance.abc123xyz.us-east-1.rds.amazonaws.com:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=MySecurePassword123!
spring.datasource.driver-class-name=org.postgresql.Driver
```

---

## Creating a New RDS PostgreSQL Instance (If Needed)

If you don't have an RDS instance yet:

1. In RDS Console, click **"Create database"**
2. Choose **"Standard create"**
3. Select **"PostgreSQL"** as the engine type
4. Choose your PostgreSQL version (recommended: latest stable version)
5. Select **"Free tier"** (if eligible) or choose your instance configuration
6. **Database settings:**
   - **DB instance identifier**: Give it a unique name (e.g., `my-postgres-db`)
   - **Master username**: Choose a username (e.g., `postgres` or `admin`)
   - **Master password**: Create a strong password (save this!)
   - **Confirm password**: Re-enter the password
7. **Instance configuration**: Choose instance size (t3.micro for free tier)
8. **Storage**: Configure storage settings
9. **Connectivity:**
   - **VPC**: Choose your VPC
   - **Public access**: Choose **"Yes"** if you want to connect from outside AWS
   - **Security group**: Create new or use existing
10. **Database name**: Enter a database name (e.g., `postgres` or `mydb`)
11. Click **"Create database"**
12. Wait 5-10 minutes for the database to be created
13. Once status shows **"Available"**, follow Steps 2-6 above to get connection details

---

## Security Best Practices

1. **Never commit passwords to Git**: Use environment variables or AWS Secrets Manager
2. **Use environment variables** (recommended):
   ```properties
   spring.datasource.url=${DB_URL}
   spring.datasource.username=${DB_USERNAME}
   spring.datasource.password=${DB_PASSWORD}
   ```
3. **Restrict security group**: Only allow connections from your application's IP/security group
4. **Use IAM database authentication** (advanced): For better security

---

## Troubleshooting

### AWS Console Errors

#### Error: "Cannot read properties of null (reading 'isFreeTier')"

This is a common AWS Console UI error. Try these solutions in order:

**Solution 1: Clear Browser Cache and Refresh**
1. Clear your browser cache and cookies for AWS Console
2. Hard refresh the page: `Ctrl + Shift + R` (Windows) or `Cmd + Shift + R` (Mac)
3. Try creating the database again

**Solution 2: Use a Different Browser**
1. Try using a different browser (Chrome, Firefox, Edge, Safari)
2. Or use an incognito/private browsing window
3. Log in to AWS Console and try again

**Solution 3: Use AWS CLI Instead**
If the console continues to fail, use AWS CLI:

```bash
aws rds create-db-instance \
    --db-instance-identifier database-1 \
    --db-instance-class db.t3.micro \
    --engine postgres \
    --master-username postgres \
    --master-user-password YourPassword123! \
    --allocated-storage 20 \
    --publicly-accessible \
    --db-name postgres
```

**Solution 4: Try Standard Create Instead of Easy Create**
1. In RDS Console, click **"Create database"**
2. Select **"Standard create"** (not "Easy create")
3. This gives you more control and may avoid the UI bug
4. Fill in all the required fields manually

**Solution 5: Check AWS Service Status**
1. Visit: https://status.aws.amazon.com/
2. Check if RDS service has any ongoing issues
3. Wait if there's a service disruption

**Solution 6: Disable Browser Extensions**
1. Disable browser extensions (especially ad blockers or privacy tools)
2. Some extensions interfere with AWS Console JavaScript
3. Try in a clean browser profile

**Solution 7: Use AWS CloudFormation or Terraform**
If console issues persist, use Infrastructure as Code:
- AWS CloudFormation template
- Terraform configuration
- AWS CDK

### Connection Issues

1. **Check Security Group Rules:**
   - Go to your RDS instance → **"Connectivity & security"** tab
   - Click on the Security Group link
   - Ensure inbound rules allow PostgreSQL (port 5432) from your IP or security group

2. **Check VPC Settings:**
   - Ensure your RDS instance is in a VPC that allows connections
   - If using public access, ensure it's enabled

3. **Verify Endpoint:**
   - Make sure you're using the correct endpoint (not the read replica endpoint)
   - Check the port number (default is 5432)

4. **Test Connection:**
   - Use a PostgreSQL client (pgAdmin, DBeaver) to test the connection
   - Or use AWS RDS Query Editor (if available)

---

## Quick Reference Checklist

- [ ] Endpoint: `your-instance.region.rds.amazonaws.com`
- [ ] Port: `5432` (default for PostgreSQL)
- [ ] Database Name: `postgres` (or your custom name)
- [ ] Username: Master username from RDS
- [ ] Password: Master password (or newly reset password)
- [ ] Security Group: Allows inbound connections on port 5432
- [ ] Public Access: Enabled (if connecting from outside AWS)

---

## Need Help?

- AWS RDS Documentation: https://docs.aws.amazon.com/rds/
- AWS Support: https://console.aws.amazon.com/support/

