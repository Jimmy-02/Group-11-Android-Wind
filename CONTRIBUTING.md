# Contributing to SciShare

Thank you for your interest in contributing to SciShare!

We welcome contributions that help improve the application's functionality, usability, performance, and overall code quality.

Please take a moment to review these guidelines before submitting your contributions.

## Getting Started

Before contributing, make sure you have:

- Android Studio installed.
- A compatible JDK.
- Android SDK configured.
- Git installed.
- Access to the SciShare repository.

### 1. Fork the Repository

Fork the repository to your own GitHub account.

### 2. Clone Your Fork

```bash
git clone https://github.com/YOUR_USERNAME/Group-11-Android-Wind.git
```

### 3. Create a Branch

Create a new branch for your changes.

```bash
git checkout -b feature/your-feature-name
```

Use descriptive branch names:

- `feature/article-search`
- `feature/user-profile`
- `fix/login-validation`
- `docs/update-readme`

## Development Guidelines

To maintain code quality and consistency, please follow these guidelines:

### Code Style

- Follow standard Java naming conventions.
- Use meaningful variable, method, and class names.
- Keep methods focused on a single responsibility.
- Avoid unnecessary code duplication.
- Add comments when the implementation requires additional explanation.
- Handle exceptions and edge cases appropriately.

### Android Development

- Follow the existing project architecture and coding conventions.
- Use appropriate Android components and lifecycle methods.
- Avoid performing heavy operations on the main thread.
- Ensure layouts work across different screen sizes.
- Handle network errors and offline scenarios gracefully.
- Test Firebase and local database interactions.

### Database and Security

- Validate user input before processing or storing data.
- Avoid exposing sensitive credentials or private information.
- Follow appropriate Firebase security rules.
- Handle database errors gracefully.

## Commit Message Guidelines

Write clear and descriptive commit messages.

We recommend the following format:

```text
<type>: <short description>
```

Common commit types:

| Type | Description |
|---|---|
| feat | Add a new feature |
| fix | Fix a bug |
| docs | Update documentation |
| style | Improve code formatting |
| refactor | Refactor existing code |
| test | Add or update tests |
| chore | Maintenance tasks |

Examples:

```bash
git commit -m "feat: add article search functionality"
git commit -m "fix: resolve login validation issue"
git commit -m "docs: update installation instructions"
```

## Testing

Before submitting your changes:

- Build the application successfully.
- Test the affected functionality.
- Check for crashes and unexpected behavior.
- Verify that existing features still work.
- Test different screen sizes when UI changes are involved.

## Pull Request Process

1. Commit your changes with descriptive messages.
2. Push your branch to your fork.

```bash
git push origin feature/your-feature-name
```

3. Open a Pull Request against the main repository.
4. Provide a clear description of your changes.
5. Include screenshots when submitting UI changes.
6. Respond to review comments and make necessary improvements.

## Reporting Bugs

If you discover a bug, please create a GitHub Issue containing:

- A clear and descriptive title.
- Steps to reproduce the problem.
- Expected behavior.
- Actual behavior.
- Screenshots or error logs, if applicable.
- Device and Android version information.

## Feature Requests

We welcome suggestions for new features and improvements.

Please describe:

- The problem you want to solve.
- Your proposed solution.
- Why the feature would be valuable to SciShare.

## Questions and Support

If you have questions or need assistance, feel free to open a GitHub Issue or contact the project maintainers.

Thank you for helping make SciShare better!
