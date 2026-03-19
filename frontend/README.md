# Turaf Frontend

Angular 17 frontend application for the Turaf platform - Problem Tracking & Solution Validation.

## Architecture

- **Framework**: Angular 17 with standalone components
- **UI Library**: Angular Material
- **State Management**: NgRx
- **Routing**: Angular Router with lazy loading
- **HTTP**: HttpClient with interceptors

## Project Structure

```
src/
├── app/
│   ├── core/              # Core services, guards, interceptors
│   ├── shared/            # Shared components, pipes, directives
│   ├── features/          # Feature modules (lazy loaded)
│   ├── models/            # TypeScript interfaces and models
│   └── store/             # NgRx state management
├── assets/                # Static assets
└── environments/          # Environment configurations
```

## Development

### Prerequisites

- Node.js 18+
- npm or yarn

### Installation

```bash
npm install
```

### Development Server

```bash
npm start
```

Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

### Build

```bash
npm run build
```

The build artifacts will be stored in the `dist/` directory.

### Running Tests

```bash
npm test
```

## Features

- **Authentication**: Login, register, password reset
- **Dashboard**: Overview of problems, hypotheses, and experiments
- **Problems**: Create and manage problem statements
- **Hypotheses**: Define hypotheses for problems
- **Experiments**: Design and run experiments
- **Metrics**: Track and visualize experiment metrics
- **Reports**: Generate and view experiment reports

## API Integration

The frontend communicates with the BFF API (Backend for Frontend) which aggregates data from multiple microservices.

- **Development**: `http://localhost:8080/api/v1`
- **Production**: `https://app.turafapp.com/api/v1`

## State Management

NgRx is used for state management with the following stores:

- Auth store
- Experiments store
- Problems store

## Contributing

Follow the Angular style guide and ensure all tests pass before submitting pull requests.
