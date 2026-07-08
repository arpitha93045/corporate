/**
 * Frontend runtime config. The Stripe publishable key is safe to ship in
 * client bundles (that's what "publishable" means) — the secret key stays
 * on the server. Replace the placeholder with your test-mode publishable
 * key ("pk_test_...") to enable the payment flow in dev.
 */
export const APP_CONFIG = {
  stripePublishableKey: 'pk_test_replace_me',
};
