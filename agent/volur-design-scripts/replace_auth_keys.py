import os
import re

root = r"H:\HuMoLix\github\Volur-Lite\src\volur-lite-view-NEXT\src"

key_replacements = {
    # captcha
    "auth.recaptcha_tip": "captcha.recaptcha.tip",
    "auth.recaptcha_required": "captcha.recaptcha.required",
    "auth.recaptcha_failed": "captcha.recaptcha.failed",
    "auth.recaptcha_refresh": "captcha.recaptcha.refresh",
    "auth.recaptcha_drag_slider": "captcha.recaptcha.drag_slider",
    "auth.recaptcha_click_to_refresh": "captcha.recaptcha.click_to_refresh",
    "auth.recaptcha_enter_image_text": "captcha.recaptcha.enter_image_text",
    "auth.recaptcha_puzzle_title": "captcha.recaptcha.puzzle_title",
    "auth.recaptcha_image_title": "captcha.recaptcha.image_title",
    "auth.recaptcha_not_robot": "captcha.recaptcha.not_robot",
    "auth.recaptcha_verified": "captcha.recaptcha.verified",
    "auth.recaptcha_missing_credentials": "captcha.recaptcha.missing_credentials",
    "auth.recaptcha_refresh_failed": "captcha.recaptcha.refresh_failed",
    "auth.recaptcha_verify_failed": "captcha.recaptcha.verify_failed",

    # recovery
    "auth.recovery_username_title": "auth.recovery.username_title",
    "auth.recovery_username_subtitle": "auth.recovery.username_subtitle",
    "auth.recovery_method_email": "auth.recovery.method_email",
    "auth.recovery_method_email_desc": "auth.recovery.method_email_desc",
    "auth.recovery_method_phone": "auth.recovery.method_phone",
    "auth.recovery_method_phone_desc": "auth.recovery.method_phone_desc",
    "auth.recovery_disabled_tip": "auth.recovery.disabled_tip",
    "auth.recovery_options_failed": "auth.recovery.options_failed",
    "auth.recovery_email_tip": "auth.recovery.email_tip",
    "auth.recovery_email_subtitle": "auth.recovery.email_subtitle",
    "auth.recovery_email_submit_failed": "auth.recovery.email_submit_failed",
    "auth.recovery_email_sent_title": "auth.recovery.email_sent_title",
    "auth.recovery_email_sent_desc": "auth.recovery.email_sent_desc",
    "auth.recovery_password_title": "auth.recovery.password_title",
    "auth.recovery_password_subtitle": "auth.recovery.password_subtitle",
    "auth.recovery_pwd_selection_title": "auth.recovery.password_title",
    "auth.recovery_pwd_selection_subtitle": "auth.recovery.password_subtitle",
    "auth.recovery_password_disabled_tip": "auth.recovery.password_disabled_tip",
    "auth.recovery_password_options_failed": "auth.recovery.password_options_failed",
    "auth.recovery_password_emailotp_title": "auth.recovery.emailotp_title",
    "auth.recovery_password_emailotp_subtitle": "auth.recovery.emailotp_subtitle",
    "auth.recovery_check_email_title": "auth.recovery.emailotp_title",
    "auth.recovery_email_code_subtitle": "auth.recovery.emailotp_subtitle",
    "auth.recovery_password_emailotp_desc": "auth.recovery.emailotp_desc",
    "auth.recovery_password_code_sent": "auth.recovery.code_sent",
    "auth.recovery_password_reset_title": "auth.recovery.reset_title",
    "auth.recovery_password_reset_subtitle": "auth.recovery.reset_subtitle",
    "auth.recovery_reset_pwd_title": "auth.recovery.reset_title",
    "auth.recovery_reset_pwd_subtitle": "auth.recovery.reset_subtitle",
    "auth.recovery_password_new_label": "auth.recovery.new_password_label",
    "auth.recovery_password_confirm_label": "auth.recovery.confirm_password_label",
    "auth.recovery_password_length_error": "auth.recovery.password_length_error",
    "auth.recovery_password_reset_success": "auth.recovery.reset_success",
    "auth.meta_title_recovery_username": "auth.recovery.meta_title_username",
    "auth.meta_title_recovery_email": "auth.recovery.meta_title_email",
    "auth.meta_title_recovery_password": "auth.recovery.meta_title_password",
    "auth.meta_title_recovery_emailotp": "auth.recovery.meta_title_emailotp",
    "auth.meta_title_recovery_reset": "auth.recovery.meta_title_reset",
    "auth.meta_title_recovery_challenge": "auth.recovery.meta_title_challenge",

    # signup
    "auth.signup_create_title": "auth.signup.title",
    "auth.create_volur_account": "auth.signup.title",
    "auth.signup_create_subtitle": "auth.signup.subtitle",
    "auth.signup_subtitle": "auth.signup.subtitle",
    "auth.username_placeholder": "auth.signup.username_placeholder",
    "auth.username_tip": "auth.signup.username_tip",
    "auth.email_tip": "auth.signup.email_tip",
    "auth.back_to_previous": "auth.signup.back_to_previous",
    "auth.back_to_name": "auth.signup.back_to_name",
    "auth.back_to_email": "auth.signup.back_to_email",
    "auth.enter_code_digits": "auth.signup.enter_code_digits",
    "auth.code_sent_to": "auth.signup.code_sent_to",
    "auth.resend_code": "auth.signup.resend_code",
    "auth.resend_countdown": "auth.signup.resend_countdown",
    "auth.password_hint": "auth.signup.password_hint",
    "auth.finish_signup": "auth.signup.finish_signup",
    "auth.signup_options_prompt": "auth.signup.options_prompt",
    "auth.passkey_protect_title": "auth.signup.passkey_protect_title",
    "auth.passkey_protect_desc": "auth.signup.passkey_protect_desc",
    "auth.passkey_registered_title": "auth.signup.passkey_registered_title",
    "auth.passkey_registered_desc": "auth.signup.passkey_registered_desc",
    "auth.create_passkey": "auth.signup.create_passkey",
    "auth.back_options": "auth.signup.back_options",
    "auth.totp_protect_title": "auth.signup.totp_protect_title",
    "auth.totp_protect_desc": "auth.signup.totp_protect_desc",
    "auth.totp_protect_notice": "auth.signup.totp_protect_notice",
    "auth.account_created_success": "auth.signup.account_created_success",
    "auth.password_setup_failed": "auth.signup.password_setup_failed",
    "auth.code_resent_success": "auth.signup.code_resent_success",
    "auth.resend_failed": "auth.signup.resend_failed",
    "auth.meta_title_signup": "auth.signup.meta_title",

    # signin
    "auth.signin_title": "auth.signin.title",
    "auth.signin": "auth.signin.title",
    "auth.signin_subtitle": "auth.signin.subtitle",
    "auth.use_volur_account": "auth.signin.subtitle",
    "auth.choose_account": "auth.signin.choose_account",
    "auth.choose_account_subtitle": "auth.signin.choose_account_subtitle",
    "auth.identifier_label": "auth.signin.identifier_label",
    "auth.identifier_required": "auth.signin.identifier_required",
    "auth.identifier_failed": "auth.signin.identifier_failed",
    "auth.switch_account": "auth.signin.switch_account",
    "auth.forgot_username": "auth.signin.forgot_username",
    "auth.password_label": "auth.signin.password_label",
    "auth.confirm_password_label": "auth.signin.confirm_password_label",
    "auth.show_password": "auth.signin.show_password",
    "auth.hide_password": "auth.signin.hide_password",
    "auth.forgot_password": "auth.signin.forgot_password",
    "auth.try_another_way": "auth.signin.try_another_way",
    "auth.another_way": "auth.signin.another_way",
    "auth.welcome": "auth.signin.welcome",
    "auth.security_verification_and_challenge": "auth.signin.security_verification_and_challenge",
    "auth.security_verification_prompt": "auth.signin.security_verification_prompt",
    "auth.two_step_verification": "auth.signin.two_step_verification",
    "auth.two_step_sms_tip": "auth.signin.two_step_sms_tip",
    "auth.two_step_email_tip": "auth.signin.two_step_email_tip",
    "auth.enter_code_label": "auth.signin.enter_code_label",
    "auth.trust_device_30_days": "auth.signin.trust_device_30_days",
    "auth.passkey_verify_title": "auth.signin.passkey_verify_title",
    "auth.passkey_verify_desc": "auth.signin.passkey_verify_desc",
    "auth.login_account": "auth.signin.login_account",
    "auth.attempt_time": "auth.signin.attempt_time",
    "auth.verify_method": "auth.signin.verify_method",
    "auth.passkey_method_name": "auth.signin.passkey_method_name",
    "auth.select_method_title": "auth.signin.select_method_title",
    "auth.login_success": "auth.signin.login_success",
    "auth.password_required": "auth.signin.password_required",
    "auth.password_incorrect": "auth.signin.password_incorrect",
    "auth.security_code_required": "auth.signin.security_code_required",
    "auth.security_code_invalid": "auth.signin.security_code_invalid",
    "auth.security_verify_success": "auth.signin.security_verify_success",
    "auth.password_verification": "auth.signin.password_verification",
    "auth.more_signin_options": "auth.signin.more_signin_options",
    "auth.email_verification": "auth.signin.email_verification",
    "auth.enter_email_code_label": "auth.signin.enter_email_code_label",
    "auth.selection_rejected_msg": "auth.signin.selection_rejected_msg",
    "auth.meta_title_accountchooser": "auth.signin.meta_title_accountchooser",
    "auth.meta_title_signin": "auth.signin.meta_title_signin",
    "auth.meta_title_challenge": "auth.signin.meta_title_challenge",

    # session expire
    "auth.session_expire_title": "auth.session_expired",
    "auth.session_expire_desc": "auth.session_expired_subtitle"
}

# Sort keys by length descending to avoid partial matches
sorted_keys = sorted(key_replacements.keys(), key=lambda x: -len(x))

changed_count = 0
for dirpath, _, filenames in os.walk(root):
    for f in filenames:
        if (f.endswith(".vue") or f.endswith(".ts")) and not f.endswith(".d.ts"):
            full_path = os.path.join(dirpath, f)
            with open(full_path, "r", encoding="utf-8") as fp:
                content = fp.read()
            original = content
            for old_k in sorted_keys:
                new_k = key_replacements[old_k]
                content = re.sub(rf"(['\"]){re.escape(old_k)}(['\"])", rf"\1{new_k}\2", content)
            if content != original:
                with open(full_path, "w", encoding="utf-8") as fp:
                    fp.write(content)
                changed_count += 1
                rel = os.path.relpath(full_path, root).replace("\\", "/")
                print(f"Updated keys in: {rel}")

print(f"Total files updated: {changed_count}")
