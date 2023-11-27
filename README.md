# ABAC-Demo Project

The `abac-demo` project showcases the functionality of the `abac-spring-boot-starter` and `formula-spring-boot-starter` libraries from Soarse ABAC project, showcasing a range of tests that cover diverse use-cases.

For a detailed overview of Soarse ABAC and Soarse Formula's functionalities, please view our YouTube presentation:
- **[YouTube Presentation Link](https://youtu.be/H_C7lUaVlX4)**

## Overview

### Approaches to Access Control

The Soarse ABAC is the Attribute-based Access Control (ABAC) approach implementation, which is designed to simplify and provide flexibility in access
control, surpassing traditional methods like Role-Based Access Control (RBAC) and Access Control List (ACL). ABAC's use of attributes for rules,
encompassing user, request, environment, or data objects, dramatically reduces the volume of access control rules. This attribute-centric approach
enables functionalities fundamentally unachievable with RBAC and ACL, addressing issues like Role Explosion and cumbersome list management.

### XACML Standard and Its Challenges

With the emergence of the XACML standard in 2003, the ABAC approach gained a formal structure. However, the complexity and operational bulkiness of
XACML, as highlighted in reports by authorities like the **[National Institute of Standards and Technology](https://nvlpubs.nist.gov/nistpubs/specialpublications/NIST.sp.800-162.pdf)**, posed significant challenges in development,
implementation, and operation. This complexity often discourages organizations from adopting XACML unless no simpler alternative is available.

### XACML Alternatives and Market Demand

In response, simpler ABAC solutions like Spring Security SpEL, jCasbin, and EasyABAC have emerged. These alternatives, though more lightweight, often
sacrifice significant functionality, particularly in data access control. The market's inclination towards these simpler systems, as indicated by the
adoption by various companies, showcases the demand for less cumbersome ABAC systems.

## Advantages of Soarse ABAC

### Simplicity of the Model

Soarse ABAC stands out by offering an uncompromisingly complete ABAC functionality within an exceptionally simple proprietary model. It avoids the
pitfalls of both complex XACML-based systems and the functionally limited lightweight alternatives.

### Flexibility and Granularity

Soarse ABAC's flexibility allows rules to depend on various attributes, supporting complex restrictions. The granularity of access control in Soarse ABAC
extends to actions, data sets, data impact types, records, and even individual record fields, providing a nuanced access control experience.

### Centralized Access Management and Performance

Unlike many other access control systems that distribute rules across the code, Soarse ABAC centralizes rule creation and storage in a policy service, allowing
real-time updates without code modifications. Its performance is optimized by executing rules either in-service memory or by modifying SQL queries,
minimizing the impact on application performance.

### Non-Invasiveness

Soarse ABAC non-invasive nature aligns with frameworks like Spring, maintaining a clear separation between business logic and infrastructure code.

## Documentation
To understand the background and architecture of the project, please refer to the following documents:

### **Soarse ABAC Architecture**

- [Positioning of Soarse ABAC](https://drive.google.com/file/d/1qKoPvp-0JQvDT0ZqykFxGdLkhdAzZ1US/view?usp=drive_link)
- [XACML Analysis](https://drive.google.com/file/d/1SAMqPb4akguPs4RUvUGxx-LB2qdGLZ3X/view?usp=sharing)
- [Integration with Soarse ABAC](https://drive.google.com/file/d/14yMHUujAFE05Pyl5_lQTjKHnmTpnPUet/view?usp=sharing)

### **ABAC Library**

- [Applying ABAC](https://drive.google.com/file/d/1YrDyqgSL9Acpq5rDltVP9j4LjHHBApKX/view?usp=sharing)
- [Permission Functions](https://drive.google.com/file/d/1Mu8HhBUfpI05C2yMxNJ5RlMy5XvOFtMx/view?usp=sharing)
- [Filtering Functions](https://drive.google.com/file/d/1GDRj3QV9W9kNSuufi7boTirQqh8tU-eq/view?usp=sharing)

### **Formula Library**

- [Formula Structure](https://drive.google.com/file/d/1x83c6qpJYCsh04N_7P87GM77uwsPz28-/view?usp=sharing)
- [Mechanism of Filtering](https://drive.google.com/file/d/13gTzW7anACbS7dQGdK_mZQZABnJYDxtM/view?usp=sharing)

## Installation and Running Instructions

To set up and run the `abac-demo` project on a Windows operating system, follow these steps:

1. Install **[Open JDK](https://openjdk.org/)**, version 17 or higher.
2. Install the latest version of **[Maven](https://maven.apache.org/)**.
3. Install a Docker option as per **[Testcontainers requirements](https://www.testcontainers.org/supported_docker_environment/)**. For example, WSL
   and Docker Desktop.
4. Clone the project from GitHub:
   ```bash
   git clone https://github.com/kirill-omelyanov/abac-demo.git
   ```
5. Copy the **[settings.xml](https://drive.google.com/file/d/1SCalUNzhgkOECBvd4XUo7xqDanSe59W2/view?usp=sharing)** file to the root folder of the abac-demo project.
6. Build the project using Maven:
   ```bash
    mvn package -s settings.xml -DTEST_POSTGRESQL_IMAGE=postgres:12.1-alpine
   ```
   Upon successful build, all tests should pass, and an executable jar file will be created.
