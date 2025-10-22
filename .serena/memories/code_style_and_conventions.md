# Code Style and Conventions

## General Conventions
- **Code Style**: Official Kotlin code style (`kotlin.code.style=official` in gradle.properties)
- **Language**: Kotlin (targeting JavaScript)
- **Framework**: Kobweb + Compose HTML

## Naming Conventions

### Functions
- **Composable functions**: PascalCase (e.g., `HomePage`, `PageLayout`, `NavHeader`)
- **Regular functions**: camelCase (e.g., `initColorMode`, `initStyles`, `toSitePalette`)
- **Initialization functions**: Prefix with `init` (e.g., `initTheme`, `initHomePage`, `initSiteStyles`)

### Classes and Data Types
- **Classes**: PascalCase (e.g., `SitePalette`, `PageLayoutData`)
- **Objects**: PascalCase (e.g., `SitePalettes`)
- **Enums**: PascalCase (e.g., `SideMenuState`)

### Properties and Variables
- **Properties**: PascalCase for style definitions (e.g., `NavHeaderStyle`, `HeadlineTextStyle`)
- **Variables**: camelCase for regular variables (e.g., `colorMode`, `sitePalette`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `COLOR_MODE_KEY`)

### Component Variants
- Suffix with "Variant" (e.g., `CircleButtonVariant`, `UncoloredButtonVariant`)

## Kobweb-Specific Annotations

### Key Annotations
- `@App`: Marks the application entry point (AppEntry.kt)
- `@Page`: Marks a composable as a routable page
- `@Layout`: Specifies layout to use for a page or marks a layout component
- `@Composable`: Standard Compose annotation for composable functions

### Layout References
- Layout references in `@Layout` annotation use dot-separated paths relative to package
- Example: `@Layout(".components.layouts.PageLayout")`

## File Organization

### File Naming
- File names match the primary component/class name
- One primary component per file

### Directory Structure
- `pages/`: Page-level components (annotated with @Page)
- `components/layouts/`: Layout components
- `components/sections/`: Larger reusable sections
- `components/widgets/`: Small reusable widgets

## Styling Conventions

### Style Definitions
- Style properties are defined as `val` with descriptive names
- Suffix with "Style" for ComponentStyle definitions
- Use `toModifier()` to convert styles to Modifiers
- Use `toAttrs()` to convert styles to HTML attributes

### Color Management
- Use ColorMode for theme switching (light/dark)
- Define custom palettes in SiteTheme.kt
- Access current palette with `ColorMode.current.toSitePalette()`

### CSS Units
- Use type-safe CSS units: `.px`, `.cssRem`, `.vh`, `.fr`
- Example: `2.cssRem`, `100.vh`, `0.5.px`

## Compose HTML Patterns

### Layout
- Use Compose layout primitives: `Box`, `Column`, `Row`
- Use `Div()` for custom HTML div elements
- Apply modifiers for styling: `.fillMaxSize()`, `.gridTemplateRows()`, etc.

### State Management
- Use `LaunchedEffect` for side effects
- Use `rememberPageContext()` for routing context
- Save persistent state to localStorage when appropriate

## Best Practices
- Keep composable functions focused and single-purpose
- Extract reusable components into separate files
- Use `Modifier` chains for styling
- Leverage Kobweb's CSS-in-Kotlin DSL for type-safe styling
- Support both light and dark color modes
- Use responsive design patterns (e.g., `.displayIfAtLeast(Breakpoint.MD)`)
