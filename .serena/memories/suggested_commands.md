# Suggested Commands

## Development Commands

### Start Development Server
```bash
cd site
kobweb run
```
- Starts local development server at http://localhost:8080
- Enables live reload for code changes
- Press 'Q' in terminal to stop server

### Build and Export

#### Export for Static Hosting
```bash
cd site
kobweb export --layout static
```
- Generates static files for deployment
- Compatible with GitHub Pages, Netlify, Firebase, etc.

#### Run Production Build Locally
```bash
cd site
kobweb run --env prod --layout static
```
- Tests production build locally before deployment

## Gradle Commands

### Build Project
```bash
./gradlew build
```
- Compiles Kotlin/JS code
- Processes resources
- Runs all build tasks

### Clean Build
```bash
./gradlew clean build
```
- Removes build artifacts
- Performs fresh build

### Run Specific Tasks
```bash
./gradlew tasks
```
- Lists all available Gradle tasks

## Git Commands
Standard Git workflow applies:
```bash
git status          # Check repository status
git add .           # Stage changes
git commit -m "..."  # Commit changes
git push            # Push to remote
git pull            # Pull from remote
```

## System Utilities (Linux)
- `ls`: List directory contents
- `cd`: Change directory
- `pwd`: Print working directory
- `grep`: Search text patterns
- `find`: Find files
- `cat`: Display file contents
- `mkdir`: Create directory
- `rm`: Remove files/directories
- `cp`: Copy files
- `mv`: Move/rename files

## IDE Recommendation
- **IntelliJ IDEA Community Edition** (via JetBrains Toolbox App)
- Provides excellent Kotlin and Gradle support

## Notes
- No dedicated test, lint, or format commands are configured in this project
- The project uses Gradle's configuration cache and build cache for faster builds
- Kobweb handles most of the build complexity through its Gradle plugins
