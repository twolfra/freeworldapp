import { Link } from 'react-router-dom';
import styles from './Legal.module.css';

export default function Impressum() {
  return (
    <main className={styles.wrap}>
      <div className={styles.inner}>
        <h1>Impressum</h1>

        <h2>Angaben gemäß § 5 TMG</h2>
        <p>
          Tim Wolfram<br />
          Torgauer Str. 20<br />
          04315 Leipzig<br />
          Deutschland
        </p>

        <h2>Kontakt</h2>
        <p>
          Kontakt: <Link to="/contact">Kontaktformular</Link>
        </p>

        <h2>Verantwortlich für den Inhalt nach § 55 Abs. 2 RStV</h2>
        <p>
          Tim Wolfram<br />
          Torgauer Str. 20<br />
          04315 Leipzig
        </p>

        <h2>Haftungsausschluss</h2>

        <h3>Haftung für Inhalte</h3>
        <p>
          Die Inhalte dieser Seiten wurden mit größter Sorgfalt erstellt. Für die
          Richtigkeit, Vollständigkeit und Aktualität der Inhalte können wir jedoch
          keine Gewähr übernehmen. Als Diensteanbieter sind wir gemäß § 7 Abs. 1 TMG
          für eigene Inhalte auf diesen Seiten nach den allgemeinen Gesetzen
          verantwortlich. Nach §§ 8 bis 10 TMG sind wir als Diensteanbieter jedoch
          nicht verpflichtet, übermittelte oder gespeicherte fremde Informationen zu
          überwachen oder nach Umständen zu forschen, die auf eine rechtswidrige
          Tätigkeit hinweisen.
        </p>

        <h3>Haftung für Links</h3>
        <p>
          Unser Angebot enthält Links zu externen Websites Dritter, auf deren Inhalte
          wir keinen Einfluss haben. Deshalb können wir für diese fremden Inhalte auch
          keine Gewähr übernehmen. Für die Inhalte der verlinkten Seiten ist stets der
          jeweilige Anbieter oder Betreiber der Seiten verantwortlich. Die verlinkten
          Seiten wurden zum Zeitpunkt der Verlinkung auf mögliche Rechtsverstöße
          überprüft. Rechtswidrige Inhalte waren zum Zeitpunkt der Verlinkung nicht
          erkennbar.
        </p>

        <h3>Urheberrecht</h3>
        <p>
          Die durch die Seitenbetreiber erstellten Inhalte und Werke auf diesen Seiten
          unterliegen dem deutschen Urheberrecht. Die Vervielfältigung, Bearbeitung,
          Verbreitung und jede Art der Verwertung außerhalb der Grenzen des
          Urheberrechtes bedürfen der schriftlichen Zustimmung des jeweiligen Autors
          bzw. Erstellers.
        </p>
      </div>
    </main>
  );
}
