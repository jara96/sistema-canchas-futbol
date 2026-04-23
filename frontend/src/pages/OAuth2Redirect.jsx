import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function OAuth2Redirect() {
  const [params] = useSearchParams()
  const nav = useNavigate()
  const { setTokenFromOAuth } = useAuth()

  useEffect(() => {
    const token = params.get('token')
    if (token) {
      setTokenFromOAuth(token).then(() => nav('/'))
    } else {
      nav('/login')
    }
  }, [])

  return <p className="text-center mt-10">Procesando login...</p>
}
