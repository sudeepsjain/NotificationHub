# Contributing to NotificationHub

Thank you for your interest in contributing to NotificationHub! We welcome contributions from the community and are grateful for your help in making this app better.

## 📋 Table of Contents
1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Setup](#development-setup)
4. [Contributing Guidelines](#contributing-guidelines)
5. [Pull Request Process](#pull-request-process)
6. [Issue Reporting](#issue-reporting)
7. [Code Style](#code-style)
8. [Testing](#testing)

## 🤝 Code of Conduct

### Our Pledge
We are committed to making participation in our project a harassment-free experience for everyone, regardless of age, body size, disability, ethnicity, gender identity and expression, level of experience, nationality, personal appearance, race, religion, or sexual identity and orientation.

### Expected Behavior
- Use welcoming and inclusive language
- Be respectful of differing viewpoints and experiences
- Gracefully accept constructive criticism
- Focus on what is best for the community
- Show empathy towards other community members

### Unacceptable Behavior
- Trolling, insulting/derogatory comments, and personal attacks
- Public or private harassment
- Publishing others' private information without explicit permission
- Other conduct which could reasonably be considered inappropriate

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or later
- Git
- Android SDK with API level 26-34

### Fork and Clone
1. Fork the repository on GitHub
2. Clone your fork locally:
```bash
git clone https://github.com/yourusername/notificationhub.git
cd notificationhub
```

3. Add the upstream repository:
```bash
git remote add upstream https://github.com/teckgrow/notificationhub.git
```

## 🛠️ Development Setup

### 1. Environment Setup
```bash
# Install Android Studio
# Download from: https://developer.android.com/studio

# Set up Android SDK
# Install SDK platforms: API 26-34
# Install build tools: 33.0.0+
```

### 2. Project Setup
```bash
# Open project in Android Studio
# Sync Gradle files
# Run initial build
./gradlew build
```

### 3. Device/Emulator Setup
```bash
# Create an emulator with API 26+ or connect physical device
# Enable USB debugging
# Grant notification access permission for testing
```

## 📝 Contributing Guidelines

### Types of Contributions

#### 🐛 Bug Fixes
- Search existing issues before creating new ones
- Include detailed reproduction steps
- Provide device/OS information
- Test your fix thoroughly

#### ✨ New Features
- Discuss major features in an issue first
- Follow the existing architecture patterns
- Maintain privacy-first principles
- Update documentation

#### 📚 Documentation
- Improve existing documentation
- Add code comments
- Create tutorials or guides
- Fix typos and grammar

#### 🧪 Testing
- Add unit tests for new functionality
- Improve test coverage
- Create integration tests
- Manual testing on different devices

### Development Workflow

#### 1. Create a Feature Branch
```bash
git checkout -b feature/your-feature-name
# or
git checkout -b bugfix/issue-number-description
```

#### 2. Make Your Changes
- Write clean, readable code
- Follow Kotlin conventions
- Add appropriate comments
- Update tests as needed

#### 3. Test Your Changes
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Manual testing
# - Install on device
# - Test core functionality
# - Verify permissions work
# - Check battery optimization
```

#### 4. Commit Your Changes
```bash
git add .
git commit -m "feat: add notification grouping feature"
# Use conventional commits format
```

#### 5. Push and Create PR
```bash
git push origin feature/your-feature-name
# Create pull request on GitHub
```

## 🔄 Pull Request Process

### Before Submitting
- [ ] Code follows the style guidelines
- [ ] Self-review completed
- [ ] Tests added/updated and passing
- [ ] Documentation updated if needed
- [ ] No merge conflicts with main branch
- [ ] PR description clearly explains changes

### PR Template
```markdown
## Description
Brief description of changes made.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
- [ ] Tested on multiple devices/OS versions

## Screenshots (if applicable)
Include screenshots for UI changes.

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No breaking changes (or clearly documented)
```

### Review Process
1. **Automated Checks**: CI/CD pipeline runs tests
2. **Code Review**: Maintainers review code
3. **Testing**: Reviewers test functionality
4. **Approval**: At least one maintainer approval required
5. **Merge**: Squash and merge to main branch

## 🐛 Issue Reporting

### Bug Reports
Use the bug report template and include:
- **Device Information**: Model, Android version, app version
- **Steps to Reproduce**: Clear, numbered steps
- **Expected Behavior**: What should happen
- **Actual Behavior**: What actually happens
- **Screenshots/Logs**: Visual evidence if applicable
- **Additional Context**: Any other relevant information

### Feature Requests
Use the feature request template and include:
- **Problem Description**: What problem does this solve?
- **Proposed Solution**: Detailed description of the feature
- **Alternatives Considered**: Other solutions you've considered
- **Additional Context**: Mockups, examples, or references

### Security Issues
**Do not open public issues for security vulnerabilities.**
Instead, email: security@teckgrow.com

## 🎨 Code Style

### Kotlin Style Guide
Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html):

#### Naming Conventions
```kotlin
// Classes: PascalCase
class NotificationProcessor

// Functions and variables: camelCase
fun processNotification()
val isImportant = true

// Constants: UPPER_SNAKE_CASE
const val MAX_NOTIFICATIONS = 100

// Package names: lowercase
package com.notificationhub.ui.fragment
```

#### Code Formatting
```kotlin
// Use meaningful names
class NotificationRepository(
    private val notificationDao: NotificationDao,
    private val preferenceManager: PreferenceManager
) {
    
    suspend fun getUnreadImportantNotifications(): List<NotificationEntity> {
        return notificationDao.getUnreadImportant()
            .filter { it.isImportant }
            .sortedByDescending { it.timestamp }
    }
}
```

#### Architecture Guidelines
- **MVVM Pattern**: Follow the established MVVM architecture
- **Repository Pattern**: Use repositories for data access
- **Dependency Injection**: Use manual DI through Application class
- **Error Handling**: Proper exception handling and user feedback

### XML Style Guide
```xml
<!-- Use consistent indentation (4 spaces) -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">
    
    <!-- Use meaningful IDs -->
    <TextView
        android:id="@+id/notification_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="16sp"
        android:textColor="@color/primary_text" />
        
</LinearLayout>
```

### Comments and Documentation
```kotlin
/**
 * Processes incoming notifications and determines their importance.
 * 
 * @param notification The notification to process
 * @param appPreference User's preference for this app
 * @return ProcessingResult containing filtered notification data
 */
fun processNotification(
    notification: StatusBarNotification,
    appPreference: AppPreferenceEntity
): ProcessingResult {
    // Check if notification should be filtered out
    if (shouldFilterNotification(notification)) {
        return ProcessingResult.Filtered
    }
    
    // Process important notifications
    return when (appPreference.isImportant) {
        true -> ProcessingResult.Important(extractNotificationData(notification))
        false -> ProcessingResult.Regular(extractNotificationData(notification))
    }
}
```

## 🧪 Testing

### Unit Tests
```kotlin
@Test
fun `processNotification should mark WhatsApp notifications as important`() {
    // Given
    val notification = createTestNotification("com.whatsapp")
    val appPreference = AppPreferenceEntity(
        packageName = "com.whatsapp",
        appName = "WhatsApp",
        isImportant = true
    )
    
    // When
    val result = notificationProcessor.processNotification(notification, appPreference)
    
    // Then
    assertTrue(result is ProcessingResult.Important)
}
```

### Integration Tests
```kotlin
@Test
fun `notification repository should store and retrieve notifications correctly`() = runTest {
    // Given
    val notification = NotificationEntity(
        id = "test_id",
        packageName = "com.test",
        appName = "Test App",
        title = "Test",
        text = "Test message",
        timestamp = System.currentTimeMillis()
    )
    
    // When
    repository.insertNotification(notification)
    val retrieved = repository.getNotificationById("test_id")
    
    // Then
    assertEquals(notification.title, retrieved?.title)
    assertEquals(notification.text, retrieved?.text)
}
```

### Manual Testing Checklist
- [ ] App installation and first launch
- [ ] Permission grant flow
- [ ] Notification filtering works correctly
- [ ] Re-alerts trigger at specified intervals
- [ ] Settings persist correctly
- [ ] Dark theme displays properly
- [ ] Multi-language support works
- [ ] App works after device restart
- [ ] Battery optimization doesn't break functionality
- [ ] Performance is acceptable on low-end devices

## 📚 Documentation Guidelines

### Code Documentation
- Document all public APIs
- Explain complex algorithms
- Include usage examples
- Keep comments up to date

### README Updates
- Update feature lists for new functionality
- Add new setup instructions
- Include new screenshots
- Update version information

### Technical Documentation
- Architecture changes
- New dependencies
- Database schema changes
- API modifications

## 🏷️ Commit Message Convention

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `test`: Adding missing tests or correcting existing tests
- `chore`: Changes to build process or auxiliary tools

### Examples
```
feat(notification): add notification grouping by app
fix(database): resolve migration issue from v1 to v2
docs(readme): update installation instructions
test(repository): add unit tests for notification filtering
```

## 🎯 Areas for Contribution

### High Priority
- [ ] Notification grouping and bundling
- [ ] Advanced filtering rules
- [ ] Accessibility improvements
- [ ] Performance optimizations
- [ ] Battery usage optimization

### Medium Priority
- [ ] Widget support
- [ ] Backup/restore functionality
- [ ] Additional language support
- [ ] UI/UX improvements
- [ ] Testing coverage improvements

### Low Priority
- [ ] Wear OS support
- [ ] Advanced statistics
- [ ] Themes and customization
- [ ] Integration with other apps

## 📞 Getting Help

### Questions and Discussions
- **GitHub Discussions**: For general questions and ideas
- **GitHub Issues**: For bug reports and feature requests
- **Code Review**: Ask questions in pull request comments

### Communication Channels
- **Email**: developers@teckgrow.com
- **GitHub**: @teckgrow-consultancy

## 🙏 Recognition

Contributors will be recognized in:
- **CONTRIBUTORS.md** file
- **Release notes** for significant contributions
- **About section** in the app
- **README.md** acknowledgments

Thank you for contributing to NotificationHub! 🎉

---

**By contributing to NotificationHub, you agree that your contributions will be licensed under the same license as the project (MIT License).**