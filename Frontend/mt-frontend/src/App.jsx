import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom'
import MovieSearch from './MovieSearch'
import MovieRecommend from './MovieRecommend'
import MovieSimilar from './MovieSimilar'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<MovieSearch />} />
        <Route path="/recommend" element={<MovieRecommend />} />
        <Route path="/from2recommend" element={<MovieSimilar />} />
      </Routes>
    </Router>
  )
}

export default App