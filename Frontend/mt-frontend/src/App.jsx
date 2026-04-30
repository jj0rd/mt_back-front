import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom'
import MovieSearch from './MovieSearch'
import MovieRecommend from './MovieRecommend'
import MovieSimilar from './MovieSimilar'
import Login from './Login'
import Register from './Register'
import RateMovie from './RateMovie'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<MovieSearch />} />
        <Route path="/recommend" element={<MovieRecommend />} />
        <Route path="/from2recommend" element={<MovieSimilar />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/rateMovie" element={<RateMovie />} />
      </Routes>
    </Router>
  )
}

export default App