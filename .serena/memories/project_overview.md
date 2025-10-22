# Project Overview: niconico-advertiser-list2

## Purpose
A web application project named "niconico_advertiser_list2", likely related to displaying or managing advertiser information for Niconico (a Japanese video streaming platform).

## Technology Stack

### Core Framework
- **Kobweb** (v0.23.3): A Kotlin/JS web framework built on Compose HTML
- **Kotlin** (v2.2.20): Primary programming language
- **JetBrains Compose** (v1.8.0): For building UI components

### Key Libraries
- **Kobweb Core**: Core framework functionality
- **Kobweb Silk**: UI component library with styling utilities
- **Compose HTML Core**: HTML rendering with Compose
- **Compose Runtime**: Compose runtime library
- **KobwebX Markdown**: Markdown support for content pages

### Build System
- **Gradle** (Kotlin DSL): Build automation
- Kotlin Multiplatform plugin
- Compose Compiler plugin

### Platform
- Frontend-only web application (Kotlin/JS)
- Static site generation capability
- No backend server component (commented out in build.gradle.kts)

## Project Type
This is a Single Page Application (SPA) bootstrapped from the Kobweb "app" template, designed for static hosting on platforms like GitHub Pages, Netlify, or Firebase.
