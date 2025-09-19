# Angular Admin Dashboard SPA

A modern, responsive Angular Single Page Application with admin dashboard functionality.

## Features

### 🎨 Admin Layout
- **Collapsible Sidebar**: Left sidebar with navigation menu that can be collapsed/expanded
- **Mobile Overlay**: Responsive design with mobile-friendly sidebar overlay
- **Header Navigation**: Top header with sidebar toggle, search, notifications, and user menu
- **Breadcrumb Navigation**: Dynamic breadcrumb for page navigation
- **Router Outlet**: Main content area that renders different pages

### 📊 Dashboard
- **Statistics Cards**: 4 cards showing key metrics with icons and percentage badges
- **Charts**: Monthly revenue bar chart and income overview pie chart (Chart.js ready)
- **Recent Orders Table**: Data table with status indicators and responsive design
- **Transaction History**: Sidebar with transaction list and avatar indicators
- **Help & Support**: Support team avatars and contact buttons

### 🔐 Authentication
- **Login Page**: Email/password form with "Keep me signed in" and "Forgot Password" options
- **Register Page**: Complete registration form with first/last name, company, email, password confirmation
- **Social Login**: Google, Twitter, and Facebook login buttons (ready for integration)
- **Form Validation**: Comprehensive form validation with error messages

### 🎯 Guest Layout
- **Clean Design**: Simple layout for authentication pages
- **Responsive**: Mobile-friendly design with proper spacing

## Technology Stack

- **Angular 17**: Latest Angular framework with standalone components
- **Bootstrap 5**: CSS framework for responsive design and components
- **Ant Design Icons**: Beautiful icon library
- **Chart.js**: Ready for chart integration (ng2-charts)
- **TypeScript**: Type-safe development
- **SCSS**: Enhanced CSS with variables and mixins

## Project Structure

```
src/app/
├── components/           # Reusable components
│   ├── card/            # Statistics card component
│   ├── breadcrumb/      # Breadcrumb navigation
│   ├── navigation/      # Sidebar navigation
│   ├── navbar/          # Header navigation
│   └── charts/          # Chart components
├── layouts/             # Layout components
│   ├── admin-layout/    # Admin dashboard layout
│   └── guest-layout/    # Guest/auth layout
├── pages/               # Page components
│   ├── dashboard/       # Main dashboard
│   ├── login/           # Login page
│   └── register/        # Registration page
├── services/            # Angular services
├── guards/              # Route guards
└── app.routes.ts        # Routing configuration
```

## Getting Started

1. **Install Dependencies**
   ```bash
   npm install
   ```

2. **Start Development Server**
   ```bash
   npm start
   ```

3. **Access the Application**
   - Open http://localhost:4200
   - Default route redirects to login page
   - After login, redirects to admin dashboard

## Routes

- `/login` - Login page (Guest layout)
- `/register` - Registration page (Guest layout)
- `/admin/dashboard` - Main dashboard (Admin layout)
- `/admin/home` - Home page (Admin layout)
- `/admin/profile` - Profile page (Admin layout)

## Components

### Shared Components
- **app-card**: Reusable card component for statistics and content
- **app-breadcrumb**: Dynamic breadcrumb navigation
- **app-navigation**: Sidebar navigation with menu items
- **app-navbar**: Header with search, notifications, and user menu
- **app-bar-chart**: Bar chart component (Chart.js ready)
- **app-pie-chart**: Pie chart component (Chart.js ready)

### Layout Components
- **app-admin-layout**: Complete admin layout with sidebar, header, and main content
- **app-guest-layout**: Simple layout for authentication pages

## Styling

- **Bootstrap Grid**: Responsive grid system
- **Bootstrap Utilities**: Spacing, colors, and utility classes
- **Custom SCSS**: Enhanced styling with CSS variables
- **Ant Design Icons**: Consistent iconography
- **Responsive Design**: Mobile-first approach

## Future Enhancements

- [ ] Chart.js integration for real charts
- [ ] Social authentication implementation
- [ ] Real-time notifications
- [ ] Dark mode toggle
- [ ] Advanced data tables with sorting/filtering
- [ ] User profile management
- [ ] Settings page
- [ ] API integration

## Development Notes

- All components are standalone (no NgModules)
- Uses Angular 17+ features and best practices
- TypeScript strict mode enabled
- SCSS for enhanced styling capabilities
- Bootstrap 5 for responsive design
- Ready for Chart.js integration