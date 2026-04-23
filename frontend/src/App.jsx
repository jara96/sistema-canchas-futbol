import { Routes, Route } from 'react-router-dom'
import Navbar from './components/Navbar'
import PrivateRoute from './components/PrivateRoute'
import Login from './pages/Login'
import Register from './pages/Register'
import OAuth2Redirect from './pages/OAuth2Redirect'
import Canchas from './pages/Canchas'
import Reservar from './pages/Reservar'
import MisReservas from './pages/MisReservas'
import Admin from './pages/Admin'

export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<Canchas />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/oauth2/redirect" element={<OAuth2Redirect />} />

        <Route path="/reservar/:canchaId" element={
          <PrivateRoute><Reservar /></PrivateRoute>
        } />
        <Route path="/mis-reservas" element={
          <PrivateRoute><MisReservas /></PrivateRoute>
        } />
        <Route path="/admin" element={
          <PrivateRoute adminOnly><Admin /></PrivateRoute>
        } />
      </Routes>
    </>
  )
}
