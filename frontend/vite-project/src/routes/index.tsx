import { Navigate, createBrowserRouter } from 'react-router-dom'
import AppLayout from '../components/layouts/AppLayout'
import LoginScreen from '../pages/auth/LoginScreen'
import { ROUTES } from './paths'
import PlansScreen from '../pages/plan/PlansScreen'
import AccountsScreen from '../pages/accounts/AccountsScreen'

export const router = createBrowserRouter([
  {
    path: ROUTES.login,
    element: <LoginScreen />,
  },
  {
    path: '/',
    element: <AppLayout />,
    children: [
      {
        index: true,
        element: <Navigate replace to={ROUTES.account} />,
      },
      {
        path: ROUTES.plan,
        element: <PlansScreen />
      },
      {
        path: ROUTES.account,
        element: <AccountsScreen />
      }
    ],
  },
  {
    path: "*",
    element: <Navigate replace to={ROUTES.account} />,
  }
])