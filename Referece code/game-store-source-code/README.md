# Game Store

A full-stack game catalog application built with ASP.NET Core 10 and React + TypeScript.

## Overview

GameStore is a web application that allows users to manage a catalog of video games. The project consists of:
- **Backend API**: ASP.NET Core 10 REST API with Entity Framework Core
- **Frontend**: React 19 with TypeScript, React Router, and Bootstrap
- **Database**: SQLite for local development

## Prerequisites

Before you begin, ensure you have the following installed:
- [.NET 10.0 SDK](https://dotnet.microsoft.com/download)
- [Node.js](https://nodejs.org/) (v18 or higher)
- [npm](https://www.npmjs.com/) (comes with Node.js)

## Getting Started

### 1. Backend Setup (API)

Open a terminal in the GameStore root directory, navigate to the API folder, and run the application:

```bash
cd GameStore.Api
dotnet run
```

This will automatically restore dependencies, apply database migrations, and start the API. The API will start at:
`http://localhost:5159`

You can test the API endpoints using the included `games.http` file (requires REST Client extension in VS Code).

### 2. Frontend Setup (React)

Open a new terminal in the GameStore root directory and navigate to the React folder:

```bash
cd GameStore.React
npm install
```

#### Run the Frontend

```bash
npm run dev
```

The React application will start at `http://localhost:5173` (or another port if 5173 is busy).

## Project Structure

### Backend (GameStore.Api)

```
GameStore.Api/
├── Data/                  # Database context and migrations
├── Dtos/                  # Data Transfer Objects
├── Endpoints/             # Minimal API endpoints
├── Models/                # Entity models (Game, Genre)
├── appsettings.json       # Configuration
└── Program.cs             # Application entry point
```

### Frontend (GameStore.React)

```
GameStore.React/
├── src/
│   ├── clients/           # API client services
│   ├── components/        # React components
│   ├── models/            # TypeScript interfaces
│   ├── pages/             # Page components
│   ├── App.tsx            # Main app component
│   └── main.tsx           # Application entry point
└── public/                # Static assets
```

## API Endpoints

### Games

- `GET /games` - Get all games
- `GET /games/{id}` - Get a specific game
- `POST /games` - Create a new game
- `PUT /games/{id}` - Update a game
- `DELETE /games/{id}` - Delete a game

### Genres

- `GET /genres` - Get all genres

## Technologies Used

### Backend

- ASP.NET Core 10
- Entity Framework Core
- SQLite
- Minimal APIs

### Frontend

- React 19
- TypeScript
- Vite
- React Router
- Bootstrap 5

## Development Tips

1. **Hot Reload**: Both the API and React app support hot reload during development
2. **Database**: The SQLite database file (`GameStore.db`) will be created in the API project directory
3. **API Testing**: Use the `games.http` file for quick API testing in VS Code with the REST Client extension

## Troubleshooting

### Port Conflicts

If the default ports are already in use:
- **API**: Modify `applicationUrl` in `Properties/launchSettings.json`
- **Frontend**: Vite will automatically suggest an alternative port

### Database Issues

If you encounter database errors:
```bash
cd GameStore.Api
dotnet ef database drop
dotnet ef database update
```

### Frontend Not Connecting to API

Ensure:
1. The API is running on the expected port
2. Check the API URL configuration in the React app's client files
3. CORS is properly configured in the API

## License

This project is for educational purposes.
