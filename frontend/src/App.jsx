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
import VerifyEmail from './pages/VerifyEmail';
import Impressum from './pages/Impressum';
import Datenschutz from './pages/Datenschutz';
import Terms from './pages/Terms';
import Contact from './pages/Contact';

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
  '/verify-email':  'Verify email' + SUFFIX,
  '/impressum':     'Impressum' + SUFFIX,
  '/datenschutz':   'Datenschutz' + SUFFIX,
  '/terms':         'Terms & Conditions' + SUFFIX,
  '/contact':       'Contact' + SUFFIX,
};

function resolve(path) {
  const exact = {
    '/': Home,
    '/offers': OfferList,
    '/offers/new': OfferForm,
    '/requests': RequestList,
    '/requests/new': RequestForm,
    '/messages': Inbox,
    '/subscriptions': Subscriptions,
    '/likes': Likes,
    '/register': Register,
    '/login': Login,
    '/verify-email': VerifyEmail,
    '/impressum': Impressum,
    '/datenschutz': Datenschutz,
    '/terms': Terms,
    '/contact': Contact,
  };
  if (exact[path]) return { Page: exact[path], params: {}, title: TITLES[path] };

  const offerDetail = path.match(/^\/offers\/([^/]+)$/);
  if (offerDetail) return { Page: OfferDetail, params: { id: offerDetail[1] }, title: 'Offer' + SUFFIX };

  const requestDetail = path.match(/^\/requests\/([^/]+)$/);
  if (requestDetail) return { Page: RequestDetail, params: { id: requestDetail[1] }, title: 'Request' + SUFFIX };

  const conversation = path.match(/^\/messages\/([^/]+)$/);
  if (conversation) return { Page: Conversation, params: { userId: conversation[1] }, title: 'Messages' + SUFFIX };

  const userProfile = path.match(/^\/users\/([^/]+)$/);
  if (userProfile) return { Page: UserProfile, params: { id: userProfile[1] }, title: 'Profile' + SUFFIX };

  return { Page: Home, params: {}, title: 'FreeWorld' };
}

export default function App() {
  const { Page, params, title } = resolve(window.location.pathname);
  document.title = title;

  return (
    <>
      <Navbar />
      <Page {...params} />
      <Footer />
    </>
  );
}
