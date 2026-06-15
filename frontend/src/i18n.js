const LANG_KEY = 'fw_lang';

export const getLang = () => localStorage.getItem(LANG_KEY) || 'en';

export function setLang(lang) {
  localStorage.setItem(LANG_KEY, lang);
  window.location.reload();
}

// Category display names — values sent to the API always stay English
export const CATEGORY_LABELS = {
  'Food & Drink':      { en: 'Food & Drink',       de: 'Essen & Trinken' },
  'Clothing':          { en: 'Clothing',            de: 'Kleidung' },
  'Books & Media':     { en: 'Books & Media',       de: 'Bücher & Medien' },
  'Tools & Equipment': { en: 'Tools & Equipment',   de: 'Werkzeug & Geräte' },
  'Furniture':         { en: 'Furniture',            de: 'Möbel' },
  'Electronics':       { en: 'Electronics',          de: 'Elektronik' },
  'Skills & Services': { en: 'Skills & Services',    de: 'Fähigkeiten & Dienste' },
  'Plants & Seeds':    { en: 'Plants & Seeds',       de: 'Pflanzen & Samen' },
  'Childcare':         { en: 'Childcare',            de: 'Kinderbetreuung' },
  'Transport':         { en: 'Transport',            de: 'Transport' },
  'Other':             { en: 'Other',                de: 'Sonstiges' },
};

export const tCat = (name) => CATEGORY_LABELS[name]?.[getLang()] ?? name;

const T = {
  en: {
    // Navbar
    'nav.offers': 'Offers', 'nav.requests': 'Requests',
    'nav.messages': 'Messages', 'nav.following': 'Following', 'nav.likes': 'Likes',
    'nav.signIn': 'Sign in', 'nav.join': 'Join', 'nav.signOut': 'Sign out',

    // Home
    'home.title': 'Everything here is', 'home.titleAccent': 'free',
    'home.subtitle': 'Your community for a gift economy — no prices, no trades, no money.',
    'home.searchPlaceholder': 'Search listings, categories, locations…',
    'home.searchBtn': 'Search',
    'home.categories': 'Categories',
    'home.stats': 'Community stats',
    'home.statOffers': 'free offers', 'home.statRequests': 'open requests',
    'home.give': 'Give something away', 'home.ask': 'Ask for something',
    'home.recent': 'Recent listings',
    'home.allOffers': 'All offers →', 'home.allRequests': 'All requests →',
    'home.loading': 'Loading…', 'home.empty': 'No listings yet — be the first to give something!',
    'home.free': 'Free', 'home.wanted': 'Wanted',
    'home.post': 'Post',
    'home.typeListings': 'Listings', 'home.typeOffers': 'Offers', 'home.typeRequests': 'Requests',
    'home.resultsFor': 'Results for "{q}"', 'home.noResults': 'No results for "{q}".',
    'home.clearSearch': 'Show recent',

    // Lists
    'list.searchPlaceholder': 'Search by title, category…',
    'list.allRegions': 'All regions',
    'list.qty': 'qty',
    'list.pagePrev': '← Prev', 'list.pageNext': 'Next →',
    'list.pageInfo': 'Page {n} of {total}',

    'offers.heading': 'Offers', 'offers.count': '{n} available',
    'offers.cta': '+ Give something away', 'offers.noMatch': 'No offers match your search.',
    'offers.priceTag': 'Free',

    'requests.heading': 'Requests', 'requests.count': '{n} open',
    'requests.cta': '+ Ask for something', 'requests.noMatch': 'No requests match your search.',
    'requests.priceTag': 'Wanted',

    // Detail
    'detail.backOffers': '← Back to Offers', 'detail.backRequests': '← Back to Requests',
    'detail.postedBy': 'Posted by', 'detail.contact': 'Contact',
    'detail.signInContact': 'Sign in to contact',
    'detail.edit': 'Edit', 'detail.delete': 'Delete', 'detail.deleting': 'Deleting…',
    'detail.region': 'Region', 'detail.qtyAvail': 'Quantity available',
    'detail.qtyNeeded': 'Quantity needed', 'detail.posted': 'Posted',
    'detail.confirmOffer': 'Delete this offer? This cannot be undone.',
    'detail.confirmRequest': 'Delete this request? This cannot be undone.',
    'detail.deleteErr': 'Could not delete: ',
    'detail.loadErrOffer': 'Could not load offer: ',
    'detail.loadErrRequest': 'Could not load request: ',
    'detail.loading': 'Loading…',

    // Edit form
    'edit.offer': 'Edit Offer', 'edit.request': 'Edit Request',
    'edit.title': 'Title', 'edit.desc': 'Description',
    'edit.category': 'Category', 'edit.qty': 'Quantity',
    'edit.region': 'Region / Location', 'edit.photo': 'Photo',
    'edit.removePhoto': 'Remove photo',
    'edit.save': 'Save changes', 'edit.saving': 'Saving…', 'edit.cancel': 'Cancel',

    // Forms
    'form.title': 'Title', 'form.desc': 'Description',
    'form.category': 'Category', 'form.qty': 'Quantity',
    'form.region': 'Region / Location', 'form.photo': 'Photo',
    'form.photoOptional': '(optional)', 'form.photoPlaceholder': 'Click to add a photo',
    'form.categoryDefault': 'Select…',
    'form.regionPlaceholder': 'e.g. Berlin, Online, North London',
    'form.signInTitle': 'Sign in first',
    'form.signInOffer': 'You need an account to make an offer.',
    'form.signInRequest': 'You need an account to make a request.',
    'form.signIn': 'Sign in', 'form.join': 'join',

    'offerForm.heading': 'Make an Offer', 'offerForm.as': 'Offering as',
    'offerForm.titlePlaceholder': 'What are you offering?',
    'offerForm.descPlaceholder': 'Tell people more — condition, size, how to pick up, etc.',
    'offerForm.submit': 'Post Offer', 'offerForm.submitting': 'Posting…',
    'offerForm.doneTitle': 'Offer posted!',
    'offerForm.doneText': 'Thank you for giving.',
    'offerForm.doneSee': 'See all offers', 'offerForm.doneAnother': 'post another',

    'requestForm.heading': 'Make a Request', 'requestForm.as': 'Requesting as',
    'requestForm.titlePlaceholder': 'What do you need?',
    'requestForm.descPlaceholder': 'Add details that help someone understand your need.',
    'requestForm.submit': 'Post Request', 'requestForm.submitting': 'Posting…',
    'requestForm.doneTitle': 'Request posted!',
    'requestForm.doneText': 'Someone in the community may be able to help.',
    'requestForm.doneSee': 'See all requests', 'requestForm.doneAnother': 'post another',

    // Profile
    'profile.memberSince': 'Member since',
    'profile.contact': 'Contact', 'profile.signInContact': 'Sign in to contact',
    'profile.subscribe': 'Subscribe', 'profile.subscribed': 'Subscribed',
    'profile.offersSection': 'Offers', 'profile.requestsSection': 'Requests',
    'profile.noOffers': 'No offers yet.', 'profile.noRequests': 'No requests yet.',
    'profile.error': 'User not found.',
    'profile.offer': 'offer', 'profile.offers': 'offers',
    'profile.request': 'request', 'profile.requests': 'requests',

    // Inbox
    'inbox.title': 'Messages',
    'inbox.signIn': 'Please {link} to view your messages.',
    'inbox.signInLink': 'sign in',
    'inbox.empty': 'No conversations yet. Contact a user from an offer or request page.',

    // Subscriptions
    'subs.title': 'Subscriptions',
    'subs.signIn': 'Please {link} to see your subscriptions feed.',
    'subs.signInLink': 'sign in',
    'subs.emptyStart': 'Nothing here yet.',
    'subs.browseOffers': 'Browse offers', 'subs.browseOr': 'or', 'subs.browseRequests': 'requests',
    'subs.emptyEnd': 'and subscribe to users you want to follow.',
    'subs.offer': 'Offer', 'subs.request': 'Request',

    // Auth
    'register.heading': 'Join the Community',
    'register.subtitle': 'No money. No trade. Just giving.',
    'register.username': 'Username', 'register.email': 'Email',
    'register.password': 'Password', 'register.passwordHint': 'At least 6 characters',
    'register.submit': 'Create Account',
    'register.checkTitle': 'Check your inbox',
    'register.checkText': 'We sent a verification link to',
    'register.checkHint': 'Click the link in the email to activate your account, then sign in.',
    'register.checkSpam': "Didn't get it? Check your spam folder or",
    'register.checkResend': 'request a new link',
    'register.checkGoLogin': 'Go to sign in',

    'login.heading': 'Welcome back',
    'login.subtitle': 'Sign in to your FreeWorld account.',
    'login.username': 'Username', 'login.password': 'Password',
    'login.submit': 'Sign In',
    'login.noAccount': 'No account yet?', 'login.join': 'Join the community',
    'login.badCreds': 'Invalid username or password.',
    'login.unverified': 'Email not verified.',
    'login.resend': 'Resend verification email',

    'verify.verifying': 'Verifying your email…', 'verify.verifyingHint': 'Please wait a moment.',
    'verify.success': 'Email verified!', 'verify.successText': 'Your account is now active. You can sign in.',
    'verify.signIn': 'Sign in',
    'verify.invalid': 'Invalid link', 'verify.invalidDefault': 'This verification link is not valid.',
    'verify.expired': 'Link expired', 'verify.expiredText': 'This verification link has expired.',
    'verify.resendHeading': 'Verify your email',
    'verify.resendHint': 'Enter your email address to receive a new verification link.',
    'verify.expiredHint': 'Enter your email to receive a new one.',
    'verify.placeholder': 'your@email.com',
    'verify.resendBtn': 'Resend verification email', 'verify.sending': 'Sending…',
    'verify.sent': 'Verification email sent — check your inbox.',
    'verify.error': 'Something went wrong. Please try again.',

    // Likes
    'likes.title': 'Likes',
    'likes.signIn': 'Please {link} to see your likes.',
    'likes.empty': 'No liked posts yet. Browse offers or requests and like posts you love.',

    // Conversation
    'conv.back': '← Back to Messages',
    'conv.cannotSelf': 'You cannot message yourself.',
    'conv.noMessages': 'No messages yet. Say hello!',
    'conv.placeholder': 'Write a message…',
    'conv.connecting': 'Connecting…',
    'conv.send': 'Send',
    'conv.seen': 'Seen',
    'conv.signIn': 'Please {link} to send messages.',
    'conv.signInLink': 'sign in',
    'conv.connLost': 'Connection lost. Please refresh to reconnect.',
    'conv.userNotFound': 'User not found.',

    // Footer
    'footer.impressum': 'Legal Notice', 'footer.datenschutz': 'Privacy Policy', 'footer.terms': 'Terms & Conditions',
    'footer.copy': '© 2025 FreeWorld',

    // Shared
    'qty': 'qty',
  },

  de: {
    // Navbar
    'nav.offers': 'Angebote', 'nav.requests': 'Gesuche',
    'nav.messages': 'Nachrichten', 'nav.following': 'Abonniert', 'nav.likes': 'Likes',
    'nav.signIn': 'Anmelden', 'nav.join': 'Registrieren', 'nav.signOut': 'Abmelden',

    // Home
    'home.title': 'Hier ist alles', 'home.titleAccent': 'kostenlos',
    'home.subtitle': 'Deine Community für eine Schenkökonomie — keine Preise, kein Tausch, kein Geld.',
    'home.searchPlaceholder': 'Einträge, Kategorien, Orte suchen…',
    'home.searchBtn': 'Suchen',
    'home.categories': 'Kategorien',
    'home.stats': 'Community-Statistik',
    'home.statOffers': 'Angebote', 'home.statRequests': 'offene Gesuche',
    'home.give': 'Etwas verschenken', 'home.ask': 'Etwas suchen',
    'home.recent': 'Neueste Einträge',
    'home.allOffers': 'Alle Angebote →', 'home.allRequests': 'Alle Gesuche →',
    'home.loading': 'Wird geladen…', 'home.empty': 'Noch keine Einträge — sei der Erste!',
    'home.free': 'Gratis', 'home.wanted': 'Gesucht',
    'home.post': 'Einstellen',
    'home.typeListings': 'Einträge', 'home.typeOffers': 'Angebote', 'home.typeRequests': 'Gesuche',
    'home.resultsFor': 'Ergebnisse für „{q}"', 'home.noResults': 'Keine Ergebnisse für „{q}".',
    'home.clearSearch': 'Neueste zeigen',

    // Lists
    'list.searchPlaceholder': 'Titel, Kategorie suchen…',
    'list.allRegions': 'Alle Regionen',
    'list.qty': 'Anz.',
    'list.pagePrev': '← Zurück', 'list.pageNext': 'Weiter →',
    'list.pageInfo': 'Seite {n} von {total}',

    'offers.heading': 'Angebote', 'offers.count': '{n} verfügbar',
    'offers.cta': '+ Etwas verschenken', 'offers.noMatch': 'Keine Angebote gefunden.',
    'offers.priceTag': 'Gratis',

    'requests.heading': 'Gesuche', 'requests.count': '{n} offen',
    'requests.cta': '+ Etwas suchen', 'requests.noMatch': 'Keine Gesuche gefunden.',
    'requests.priceTag': 'Gesucht',

    // Detail
    'detail.backOffers': '← Zurück zu Angeboten', 'detail.backRequests': '← Zurück zu Gesuchen',
    'detail.postedBy': 'Eingestellt von', 'detail.contact': 'Kontaktieren',
    'detail.signInContact': 'Anmelden zum Kontaktieren',
    'detail.edit': 'Bearbeiten', 'detail.delete': 'Löschen', 'detail.deleting': 'Wird gelöscht…',
    'detail.region': 'Region', 'detail.qtyAvail': 'Verfügbare Menge',
    'detail.qtyNeeded': 'Benötigte Menge', 'detail.posted': 'Eingestellt',
    'detail.confirmOffer': 'Dieses Angebot löschen? Dies kann nicht rückgängig gemacht werden.',
    'detail.confirmRequest': 'Dieses Gesuch löschen? Dies kann nicht rückgängig gemacht werden.',
    'detail.deleteErr': 'Löschen fehlgeschlagen: ',
    'detail.loadErrOffer': 'Angebot konnte nicht geladen werden: ',
    'detail.loadErrRequest': 'Gesuch konnte nicht geladen werden: ',
    'detail.loading': 'Wird geladen…',

    // Edit form
    'edit.offer': 'Angebot bearbeiten', 'edit.request': 'Gesuch bearbeiten',
    'edit.title': 'Titel', 'edit.desc': 'Beschreibung',
    'edit.category': 'Kategorie', 'edit.qty': 'Menge',
    'edit.region': 'Region / Ort', 'edit.photo': 'Foto',
    'edit.removePhoto': 'Foto entfernen',
    'edit.save': 'Änderungen speichern', 'edit.saving': 'Wird gespeichert…', 'edit.cancel': 'Abbrechen',

    // Forms
    'form.title': 'Titel', 'form.desc': 'Beschreibung',
    'form.category': 'Kategorie', 'form.qty': 'Menge',
    'form.region': 'Region / Ort', 'form.photo': 'Foto',
    'form.photoOptional': '(optional)', 'form.photoPlaceholder': 'Klicken um Foto hinzuzufügen',
    'form.categoryDefault': 'Bitte wählen…',
    'form.regionPlaceholder': 'z.B. Berlin, Online, München',
    'form.signInTitle': 'Zuerst anmelden',
    'form.signInOffer': 'Du benötigst ein Konto, um ein Angebot zu erstellen.',
    'form.signInRequest': 'Du benötigst ein Konto, um ein Gesuch zu erstellen.',
    'form.signIn': 'Anmelden', 'form.join': 'registrieren',

    'offerForm.heading': 'Angebot erstellen', 'offerForm.as': 'Angebot als',
    'offerForm.titlePlaceholder': 'Was bietest du an?',
    'offerForm.descPlaceholder': 'Zustand, Größe, Abholmöglichkeiten usw.',
    'offerForm.submit': 'Angebot veröffentlichen', 'offerForm.submitting': 'Wird veröffentlicht…',
    'offerForm.doneTitle': 'Angebot erstellt!',
    'offerForm.doneText': 'Danke fürs Teilen.',
    'offerForm.doneSee': 'Alle Angebote', 'offerForm.doneAnother': 'weiteres erstellen',

    'requestForm.heading': 'Gesuch erstellen', 'requestForm.as': 'Gesuch als',
    'requestForm.titlePlaceholder': 'Was suchst du?',
    'requestForm.descPlaceholder': 'Details, die anderen helfen, deinen Bedarf zu verstehen.',
    'requestForm.submit': 'Gesuch veröffentlichen', 'requestForm.submitting': 'Wird veröffentlicht…',
    'requestForm.doneTitle': 'Gesuch erstellt!',
    'requestForm.doneText': 'Jemand in der Community kann vielleicht helfen.',
    'requestForm.doneSee': 'Alle Gesuche', 'requestForm.doneAnother': 'weiteres erstellen',

    // Profile
    'profile.memberSince': 'Mitglied seit',
    'profile.contact': 'Kontaktieren', 'profile.signInContact': 'Anmelden zum Kontaktieren',
    'profile.subscribe': 'Abonnieren', 'profile.subscribed': 'Abonniert',
    'profile.offersSection': 'Angebote', 'profile.requestsSection': 'Gesuche',
    'profile.noOffers': 'Noch keine Angebote.', 'profile.noRequests': 'Noch keine Gesuche.',
    'profile.error': 'Benutzer nicht gefunden.',
    'profile.offer': 'Angebot', 'profile.offers': 'Angebote',
    'profile.request': 'Gesuch', 'profile.requests': 'Gesuche',

    // Inbox
    'inbox.title': 'Nachrichten',
    'inbox.signIn': 'Bitte {link}, um deine Nachrichten zu sehen.',
    'inbox.signInLink': 'anmelden',
    'inbox.empty': 'Noch keine Konversationen. Kontaktiere einen Nutzer von einem Angebot oder Gesuch.',

    // Subscriptions
    'subs.title': 'Abonnements',
    'subs.signIn': 'Bitte {link}, um deinen Abonnement-Feed zu sehen.',
    'subs.signInLink': 'anmelden',
    'subs.emptyStart': 'Noch nichts hier.',
    'subs.browseOffers': 'Angebote durchstöbern', 'subs.browseOr': 'oder', 'subs.browseRequests': 'Gesuche',
    'subs.emptyEnd': 'und Nutzer abonnieren, denen du folgen möchtest.',
    'subs.offer': 'Angebot', 'subs.request': 'Gesuch',

    // Auth
    'register.heading': 'Community beitreten',
    'register.subtitle': 'Kein Geld. Kein Tausch. Nur schenken.',
    'register.username': 'Benutzername', 'register.email': 'E-Mail',
    'register.password': 'Passwort', 'register.passwordHint': 'Mindestens 6 Zeichen',
    'register.submit': 'Konto erstellen',
    'register.checkTitle': 'E-Mail prüfen',
    'register.checkText': 'Wir haben einen Bestätigungslink gesendet an',
    'register.checkHint': 'Klicke auf den Link in der E-Mail, um dein Konto zu aktivieren.',
    'register.checkSpam': 'Nichts erhalten? Spam-Ordner prüfen oder',
    'register.checkResend': 'neuen Link anfordern',
    'register.checkGoLogin': 'Zur Anmeldung',

    'login.heading': 'Willkommen zurück',
    'login.subtitle': 'In deinem FreeWorld-Konto anmelden.',
    'login.username': 'Benutzername', 'login.password': 'Passwort',
    'login.submit': 'Anmelden',
    'login.noAccount': 'Noch kein Konto?', 'login.join': 'Community beitreten',
    'login.badCreds': 'Benutzername oder Passwort falsch.',
    'login.unverified': 'E-Mail nicht bestätigt.',
    'login.resend': 'Bestätigungslink erneut senden',

    'verify.verifying': 'E-Mail wird bestätigt…', 'verify.verifyingHint': 'Bitte einen Moment warten.',
    'verify.success': 'E-Mail bestätigt!', 'verify.successText': 'Dein Konto ist jetzt aktiv. Du kannst dich anmelden.',
    'verify.signIn': 'Anmelden',
    'verify.invalid': 'Ungültiger Link', 'verify.invalidDefault': 'Dieser Bestätigungslink ist nicht gültig.',
    'verify.expired': 'Link abgelaufen', 'verify.expiredText': 'Dieser Bestätigungslink ist abgelaufen.',
    'verify.resendHeading': 'E-Mail bestätigen',
    'verify.resendHint': 'Gib deine E-Mail-Adresse ein, um einen neuen Link zu erhalten.',
    'verify.expiredHint': 'Gib deine E-Mail-Adresse für einen neuen Link ein.',
    'verify.placeholder': 'deine@email.de',
    'verify.resendBtn': 'Bestätigungslink senden', 'verify.sending': 'Wird gesendet…',
    'verify.sent': 'Bestätigungslink gesendet — E-Mails prüfen.',
    'verify.error': 'Ein Fehler ist aufgetreten. Bitte erneut versuchen.',

    // Likes
    'likes.title': 'Likes',
    'likes.signIn': 'Bitte {link}, um deine Likes zu sehen.',
    'likes.empty': 'Keine Likes noch. Angebote oder Gesuche durchstöbern und Einträge liken, die dir gefallen.',

    // Conversation
    'conv.back': '← Zurück zu Nachrichten',
    'conv.cannotSelf': 'Du kannst dir nicht selbst schreiben.',
    'conv.noMessages': 'Noch keine Nachrichten. Sag Hallo!',
    'conv.placeholder': 'Nachricht schreiben…',
    'conv.connecting': 'Verbinde…',
    'conv.send': 'Senden',
    'conv.seen': 'Gelesen',
    'conv.signIn': 'Bitte {link}, um Nachrichten zu senden.',
    'conv.signInLink': 'anmelden',
    'conv.connLost': 'Verbindung unterbrochen. Bitte Seite neu laden.',
    'conv.userNotFound': 'Benutzer nicht gefunden.',

    // Footer
    'footer.impressum': 'Impressum', 'footer.datenschutz': 'Datenschutz', 'footer.terms': 'AGB',
    'footer.copy': '© 2025 FreeWorld',

    // Shared
    'qty': 'Anz.',
  },
};

// Missing keys in DE fall back to EN automatically via t().

export function t(key) {
  const lang = getLang();
  return T[lang]?.[key] ?? T.en[key] ?? key;
}

// Interpolate {placeholders} — e.g. tp('list.pageInfo', { n: 2, total: 5 })
export function tp(key, params) {
  return Object.entries(params).reduce(
    (s, [k, v]) => s.replace(`{${k}}`, v),
    t(key)
  );
}
