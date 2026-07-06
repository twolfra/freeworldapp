import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, useLocation, useParams } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ToastProvider } from './components/ui';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import Home from './pages/Home';
import OfferList from './pages/OfferList';
import OfferForm from './pages/OfferForm';
import RequestList from './pages/RequestList';
import RequestForm from './pages/RequestForm';
import OfferDetail from './pages/OfferDetail';
import RequestDetail from './pages/RequestDetail';
import Inbox from './pages/Inbox';
import Conversation from './pages/Conversation';
import UserProfile from './pages/UserProfile';
import Subscriptions from './pages/Subscriptions';
import Likes from './pages/Likes';
import Register from './pages/Register';
import Login from './pages/Login';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import VerifyEmail from './pages/VerifyEmail';
import Impressum from './pages/Impressum';
import Datenschutz from './pages/Datenschutz';
import Terms from './pages/Terms';
import Contact from './pages/Contact';
import Admin from './pages/Admin';
import NotFound from './pages/NotFound';

const SUFFIX = ' — FreeWorld';

const TITLES = {
  '/':              'FreeWorld',
  '/offers':        'Offers' + SUFFIX,
  '/offers/new':    'Give something away' + SUFFIX,
  '/requests':      'Requests' + SUFFIX,
  '/requests/new':  'Ask for something' + SUFFIX,
  '/messages':      'Messages' + SUFFIX,
  '/subscriptions': 'Following' + SUFFIX,
  '/likes':         'Likes' + SUFFIX,
  '/register':      'Join FreeWorld',
  '/login':         'Sign in' + SUFFIX,
  '/forgot-password': 'Forgot password' + SUFFIX,
  '/reset-password':  'Reset password' + SUFFIX,
  '/verify-email':  'Verify email' + SUFFIX,
  '/impressum':     'Impressum' + SUFFIX,
  '/datenschutz':   'Datenschutz' + SUFFIX,
  '/terms':         'Terms & Conditions' + SUFFIX,
  '/contact':       'Contact' + SUFFIX,
  '/admin':         'Admin' + SUFFIX,
};

function titleFor(path) {
  if (TITLES[path]) return TITLES[path];
  if (/^\/offers\/[^/]+$/.test(path))   return 'Offer' + SUFFIX;
  if (/^\/requests\/[^/]+$/.test(path)) return 'Request' + SUFFIX;
  if (/^\/messages\/[^/]+$/.test(path)) return 'Messages' + SUFFIX;
  if (/^\/users\/[^/]+$/.test(path))    return 'Profile' + SUFFIX;
  return 'Page not found' + SUFFIX;
}

// Pages used to re-initialize their state on every navigation because each
// link was a full page reload. With client-side routing the component stays
// mounted when only a param or the query string changes, so we key the
// element to force a remount — preserving the old semantics (fresh fetch,
// reset form/search state, new WebSocket per conversation).
function Remount({ component: Component, by }) {
  const params = useParams();
  const location = useLocation();
  const key = by === 'search' ? location.search : params[by];
  return <Component key={key} />;
}

// Client-side navigation no longer reloads the page, so document.title
// must be updated whenever the location changes.
function TitleManager() {
  const { pathname } = useLocation();
  useEffect(() => {
    document.title = titleFor(pathname);
  }, [pathname]);
  return null;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <TitleManager />
          <Navbar />
          <Routes>
            <Route path="/" element={<Remount component={Home} by="search" />} />
            <Route path="/offers" element={<Remount component={OfferList} by="search" />} />
            <Route path="/offers/new" element={<OfferForm />} />
            <Route path="/offers/:id" element={<Remount component={OfferDetail} by="id" />} />
            <Route path="/requests" element={<Remount component={RequestList} by="search" />} />
            <Route path="/requests/new" element={<RequestForm />} />
            <Route path="/requests/:id" element={<Remount component={RequestDetail} by="id" />} />
            <Route path="/users/:id" element={<Remount component={UserProfile} by="id" />} />
            <Route path="/messages" element={<Inbox />} />
            <Route path="/messages/:userId" element={<Remount component={Conversation} by="userId" />} />
            <Route path="/subscriptions" element={<Subscriptions />} />
            <Route path="/likes" element={<Likes />} />
            <Route path="/register" element={<Register />} />
            <Route path="/login" element={<Login />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<Remount component={ResetPassword} by="search" />} />
            <Route path="/verify-email" element={<Remount component={VerifyEmail} by="search" />} />
            <Route path="/impressum" element={<Impressum />} />
            <Route path="/datenschutz" element={<Datenschutz />} />
            <Route path="/terms" element={<Terms />} />
            <Route path="/contact" element={<Contact />} />
            <Route path="/admin" element={<Admin />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
          <Footer />
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
