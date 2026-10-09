import json
import os

base_locales = r"H:\HuMoLix\github\Volur-Lite\src\volur-lite-view-NEXT\src\locales"
with open(os.path.join(base_locales, "zh-CN", "auth.json"), "r", encoding="utf-8") as fp:
    zh_cn = json.load(fp)

# Define exact mapping from old key to (target_file, new_key)
# target_file in: ['captcha/recaptcha', 'auth/recovery', 'auth/signup', 'auth/signin', 'auth/auth']

captcha_recaptcha = {
    'recaptcha_tip': 'tip',
    'recaptcha_required': 'required',
    'recaptcha_failed': 'failed',
    'recaptcha_refresh': 'refresh',
    'recaptcha_drag_slider': 'drag_slider',
    'recaptcha_click_to_refresh': 'click_to_refresh',
    'recaptcha_enter_image_text': 'enter_image_text',
    'recaptcha_puzzle_title': 'puzzle_title',
    'recaptcha_image_title': 'image_title',
    'recaptcha_not_robot': 'not_robot',
    'recaptcha_verified': 'verified',
    'recaptcha_missing_credentials': 'missing_credentials',
    'recaptcha_refresh_failed': 'refresh_failed',
    'recaptcha_verify_failed': 'verify_failed'
}

auth_recovery = {
    'recovery_username_title': 'username_title',
    'recovery_username_subtitle': 'username_subtitle',
    'recovery_method_email': 'method_email',
    'recovery_method_email_desc': 'method_email_desc',
    'recovery_method_phone': 'method_phone',
    'recovery_method_phone_desc': 'method_phone_desc',
    'recovery_disabled_tip': 'disabled_tip',
    'recovery_options_failed': 'options_failed',
    'recovery_email_tip': 'email_tip',
    'recovery_email_subtitle': 'email_subtitle',
    'recovery_email_submit_failed': 'email_submit_failed',
    'recovery_email_sent_title': 'email_sent_title',
    'recovery_email_sent_desc': 'email_sent_desc',
    'recovery_password_title': 'password_title',
    'recovery_password_subtitle': 'password_subtitle',
    'recovery_pwd_selection_title': 'password_title',
    'recovery_pwd_selection_subtitle': 'password_subtitle',
    'recovery_password_disabled_tip': 'password_disabled_tip',
    'recovery_password_options_failed': 'password_options_failed',
    'recovery_password_emailotp_title': 'emailotp_title',
    'recovery_password_emailotp_subtitle': 'emailotp_subtitle',
    'recovery_check_email_title': 'emailotp_title',
    'recovery_email_code_subtitle': 'emailotp_subtitle',
    'recovery_password_emailotp_desc': 'emailotp_desc',
    'recovery_password_code_sent': 'code_sent',
    'recovery_password_reset_title': 'reset_title',
    'recovery_password_reset_subtitle': 'reset_subtitle',
    'recovery_reset_pwd_title': 'reset_title',
    'recovery_reset_pwd_subtitle': 'reset_subtitle',
    'recovery_password_new_label': 'new_password_label',
    'recovery_password_confirm_label': 'confirm_password_label',
    'recovery_password_length_error': 'password_length_error',
    'recovery_password_reset_success': 'reset_success',
    'meta_title_recovery_username': 'meta_title_username',
    'meta_title_recovery_email': 'meta_title_email',
    'meta_title_recovery_password': 'meta_title_password',
    'meta_title_recovery_emailotp': 'meta_title_emailotp',
    'meta_title_recovery_reset': 'meta_title_reset',
    'meta_title_recovery_challenge': 'meta_title_challenge'
}

auth_signup = {
    'signup_create_title': 'title',
    'create_volur_account': 'title',
    'signup_create_subtitle': 'subtitle',
    'signup_subtitle': 'subtitle',
    'username_placeholder': 'username_placeholder',
    'username_tip': 'username_tip',
    'email_tip': 'email_tip',
    'back_to_previous': 'back_to_previous',
    'back_to_name': 'back_to_name',
    'back_to_email': 'back_to_email',
    'enter_code_digits': 'enter_code_digits',
    'code_sent_to': 'code_sent_to',
    'resend_code': 'resend_code',
    'resend_countdown': 'resend_countdown',
    'password_hint': 'password_hint',
    'finish_signup': 'finish_signup',
    'signup_options_prompt': 'options_prompt',
    'passkey_protect_title': 'passkey_protect_title',
    'passkey_protect_desc': 'passkey_protect_desc',
    'passkey_registered_title': 'passkey_registered_title',
    'passkey_registered_desc': 'passkey_registered_desc',
    'create_passkey': 'create_passkey',
    'back_options': 'back_options',
    'totp_protect_title': 'totp_protect_title',
    'totp_protect_desc': 'totp_protect_desc',
    'totp_protect_notice': 'totp_protect_notice',
    'account_created_success': 'account_created_success',
    'password_setup_failed': 'password_setup_failed',
    'code_resent_success': 'code_resent_success',
    'resend_failed': 'resend_failed',
    'meta_title_signup': 'meta_title'
}

auth_signin = {
    'signin_title': 'title',
    'signin': 'title',
    'signin_subtitle': 'subtitle',
    'use_volur_account': 'subtitle',
    'choose_account': 'choose_account',
    'choose_account_subtitle': 'choose_account_subtitle',
    'identifier_label': 'identifier_label',
    'identifier_required': 'identifier_required',
    'identifier_failed': 'identifier_failed',
    'switch_account': 'switch_account',
    'forgot_username': 'forgot_username',
    'password_label': 'password_label',
    'confirm_password_label': 'confirm_password_label',
    'show_password': 'show_password',
    'hide_password': 'hide_password',
    'forgot_password': 'forgot_password',
    'try_another_way': 'try_another_way',
    'another_way': 'another_way',
    'welcome': 'welcome',
    'security_verification': 'security_verification',
    'security_verification_and_challenge': 'security_verification_and_challenge',
    'security_verification_prompt': 'security_verification_prompt',
    'two_step_verification': 'two_step_verification',
    'two_step_sms_tip': 'two_step_sms_tip',
    'two_step_email_tip': 'two_step_email_tip',
    'enter_code_label': 'enter_code_label',
    'trust_device_30_days': 'trust_device_30_days',
    'passkey_verify_title': 'passkey_verify_title',
    'passkey_verify_desc': 'passkey_verify_desc',
    'login_account': 'login_account',
    'attempt_time': 'attempt_time',
    'verify_method': 'verify_method',
    'passkey_method_name': 'passkey_method_name',
    'select_method_title': 'select_method_title',
    'login_success': 'login_success',
    'password_required': 'password_required',
    'password_incorrect': 'password_incorrect',
    'security_code_required': 'security_code_required',
    'security_code_invalid': 'security_code_invalid',
    'security_verify_success': 'security_verify_success',
    'password_verification': 'password_verification',
    'more_signin_options': 'more_signin_options',
    'email_verification': 'email_verification',
    'enter_email_code_label': 'enter_email_code_label',
    'selection_rejected_msg': 'selection_rejected_msg',
    'meta_title_accountchooser': 'meta_title_accountchooser',
    'meta_title_signin': 'meta_title_signin',
    'meta_title_challenge': 'meta_title_challenge'
}

# All others go to auth_auth:
assigned = set(captcha_recaptcha.keys()) | set(auth_recovery.keys()) | set(auth_signup.keys()) | set(auth_signin.keys())
remaining = set(zh_cn.keys()) - assigned
print(f"Assigned: {len(assigned)}")
print(f"Remaining for auth/auth.json: {len(remaining)}")
print(sorted(list(remaining)))
