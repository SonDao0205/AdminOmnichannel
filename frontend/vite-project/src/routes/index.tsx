import { Navigate, createBrowserRouter } from 'react-router-dom'
import AppLayout from '../components/layouts/AppLayout'
import AnalystScreen from '../pages/analyst/AnalystScreen'
import LoginScreen from '../pages/auth/LoginScreen'
import ChatScreen from '../pages/chat/ChatScreen'
import ConnectScreen from '../pages/connect/ConnectScreen'
import CustomerScreen from '../pages/customer/CustomerScreen'
import OrderScreen from '../pages/order/OrderScreen'
import OverviewScreen from '../pages/overview/OverviewScreen'
import ProductScreen from '../pages/products/ProductScreen'
import { ROUTES } from './paths'

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
        element: <Navigate replace to={ROUTES.overview} />,
      },
      {
        path: ROUTES.overview,
        element: <OverviewScreen />,
      },
      {
        path: ROUTES.connect,
        element: <ConnectScreen />,
      },
      {
        path: ROUTES.products,
        element: <ProductScreen />,
      },
      {
        path: ROUTES.orders,
        element: <OrderScreen />,
      },
      {
        path: ROUTES.customers,
        element: <CustomerScreen />,
      },
      {
        path: ROUTES.chat,
        element: <ChatScreen />,
      },
      {
        path: ROUTES.analytics,
        element: <AnalystScreen />,
      },
    ],
  },
  {
    path: '*',
    element: <Navigate replace to={ROUTES.overview} />,
  },
])