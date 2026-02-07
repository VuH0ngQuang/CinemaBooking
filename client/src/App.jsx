import React from 'react'
import Navbar from './components/Navbar.jsx'
import {Routes, Route, useLocation} from 'react-router-dom'
import Movies from './pages/Movies.jsx'
import MovieDetails from './pages/MovieDetails.jsx'
import SeatLayout from './pages/SeatLayout.jsx'
import MyBookings from './pages/MyBookings.jsx'
import Favourite from './pages/Favourite.jsx'
import Home from './pages/Home.jsx'
import {Toaster} from 'react-hot-toast'
import Footer from './components/Footer.jsx'
import Login from './pages/Login.jsx'

const App = () => {

  const isAdminRoute = useLocation().pathname.startsWith('/admin')

  return (
    <>
      <Toaster/>
      {!isAdminRoute && <Navbar/>}
      <Routes>
        <Route path='/' element={<Home/>}/>
        <Route path='/movies' element={<Movies/>}/>
        <Route path='/movies/:id' element={<MovieDetails/>}/>
        <Route path='/movies/:id/:date' element={<SeatLayout/>}/>
        <Route path='/my-bookings' element={<MyBookings/>}/>
        <Route path='/favorite' element={<Favourite/>}/>
        <Route path="/login" element={<Login />} />
      </Routes>
      {!isAdminRoute && <Footer/>}
    </>
  )
}

export default App