# Task Completion Checklist

When completing a task on this project, follow these steps:

## Code Quality

### 1. Code Style
- [ ] Follow official Kotlin code style conventions
- [ ] Use appropriate naming conventions (see code_style_and_conventions.md)
- [ ] Keep composable functions focused and single-purpose
- [ ] Ensure proper use of Kobweb annotations (@Page, @Layout, @Composable, @App)

### 2. Theme Support
- [ ] Verify component works in both light and dark color modes
- [ ] Use `ColorMode.current.toSitePalette()` for theme-aware colors
- [ ] Test color contrast for accessibility

### 3. Responsive Design
- [ ] Test on different breakpoints (SM, MD, LG, XL)
- [ ] Use `.displayIfAtLeast()` for responsive visibility
- [ ] Ensure mobile-friendly layouts

## Testing

### Manual Testing
- [ ] Start development server: `cd site && kobweb run`
- [ ] Test in browser at http://localhost:8080
- [ ] Verify live reload works with code changes
- [ ] Test navigation between pages
- [ ] Test theme toggle (light/dark mode)

### Build Verification
- [ ] Run build: `./gradlew build`
- [ ] Ensure no compilation errors
- [ ] Check for any warnings in build output

### Production Build Testing
- [ ] Export static build: `cd site && kobweb export --layout static`
- [ ] Run production build: `cd site && kobweb run --env prod --layout static`
- [ ] Verify production build works as expected

## Documentation
- [ ] Update comments for complex logic
- [ ] Document any new components or patterns
- [ ] Update README.md if architecture changes

## Version Control
- [ ] Review git status: `git status`
- [ ] Stage changes: `git add .`
- [ ] Commit with descriptive message: `git commit -m "..."`
- [ ] Push changes: `git push` (if appropriate)

## Notes
- This project does not have automated tests configured
- No linting or formatting tools are currently set up
- Focus on manual testing through the development server
- Kobweb's live reload feature helps with rapid iteration

## Optional (for significant changes)
- [ ] Test exported static build deployment to a staging environment
- [ ] Verify all routes work correctly
- [ ] Check browser console for errors or warnings
