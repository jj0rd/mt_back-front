import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom'
import MovieSearch from './MovieSearch'
import MovieRecommend from './MovieRecommend'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<MovieSearch />} />
        <Route path="/recommend" element={<MovieRecommend />} />
      </Routes>
    </Router>
  )
}

export default App