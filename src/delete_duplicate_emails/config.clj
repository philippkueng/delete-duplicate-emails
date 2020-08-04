(ns delete-duplicate-emails.config)

(def source
  "The email account holding the emails which need to be deleted in the destination account."
  {:email "your-email@gmail.com"
   :password "your-password"
   :host "imap.gmail.com"})

(def destination
  "The email account which needs to be freed from emails already stored in the source account.
   Eg. due to space limits reached."
  {:email "your-email@outlook.com"
   :password "your-password"
   :host "outlook.office365.com"})
