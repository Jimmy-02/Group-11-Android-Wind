# Security Policy

## Supported Versions

Security updates are provided for the latest version of SciShare.

| Version | Supported |
|---|---|
| Latest | :white_check_mark: |
| Older versions | :x: |

## Reporting a Vulnerability

We take the security of SciShare seriously.

If you discover a potential security vulnerability, please report it responsibly.

**Please do not disclose security vulnerabilities publicly through GitHub Issues.**

Instead, contact the project maintainers through GitHub or use GitHub's private vulnerability reporting feature if enabled.

When reporting a vulnerability, please include:

- A clear description of the vulnerability.
- Steps to reproduce the issue.
- Potential security impact.
- Screenshots, logs, or proof of concept, if applicable.
- Suggested mitigation, if available.

## Security Best Practices

The SciShare project follows these security practices:

### Authentication and Authorization
- Use Firebase Authentication for user authentication.
- Enforce appropriate access control for application features.
- Prevent unauthorized access to administrative functionality.

### Database Security
- Apply appropriate Firebase Firestore Security Rules.
- Validate user input before storing or processing data.
- Restrict access to sensitive user information.
- Avoid exposing private data through database queries.

### Data Protection
- Never commit passwords, private keys, access tokens, or sensitive credentials.
- Keep sensitive configuration files out of public repositories when appropriate.
- Handle user information responsibly.
- Use HTTPS for network communication.

### Application Security
- Validate external URLs and downloaded content.
- Handle exceptions and network failures safely.
- Avoid exposing sensitive information in application logs.
- Keep dependencies and development tools updated.

## Scope

This security policy applies to:

- Authentication and account management.
- Scientific articles and document handling.
- Discussions and Q&A functionality.
- Administrative features and content moderation.
- Firebase services and local data storage.

## Acknowledgements

We appreciate responsible security researchers and contributors who help improve SciShare's security and protect its users.

Thank you for helping keep SciShare safe.