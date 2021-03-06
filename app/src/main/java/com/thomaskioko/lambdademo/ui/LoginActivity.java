package com.thomaskioko.lambdademo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.thomaskioko.lambdademo.R;
import com.thomaskioko.lambdademo.model.User;

import java.util.concurrent.TimeUnit;

import butterknife.BindInt;
import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static com.thomaskioko.lambdademo.utils.StringUtils.validateEmail;
import static com.thomaskioko.lambdademo.utils.StringUtils.validatePassword;


/**
 * @author kioko
 */

public class LoginActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.txt_input_layout_email)
    TextInputLayout mEmailInputLayout;
    @Bind(R.id.txt_input_layout_password)
    TextInputLayout mPasswordInputLayout;
    @Bind(R.id.et_password)
    EditText mPasswordEditText;
    @Bind(R.id.et_email)
    EditText mEmailEditText;
    @Bind(R.id.sign_in_ll)
    LinearLayout mSignInLinearLayout;
    @Bind(R.id.btn_sign_in)
    Button mButtonSignIn;
    @Bind(R.id.btn_sign_up)
    Button mButtonSignUp;
    @BindInt(R.integer.debounce_length)
    int mDebounceLength;

    protected CompositeSubscription mCompositeSubscription = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);
        getActivityComponent().inject(this);

        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.title_sign_in));
        }


        Observable<User> userObservable = mRealm.asObservable()
                .map(realm -> realm.where(User.class).findAll().first())
                .filter(user -> user != null)
                .debounce(mDebounceLength, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread()); // UI Thread;


        Observable<CharSequence> emailObservable = RxTextView.textChanges(mEmailEditText)
                .filter(charSequence -> !TextUtils.isEmpty(charSequence));
        Observable<CharSequence> passwordObservable = RxTextView.textChanges(mPasswordEditText)
                .filter(charSequence -> !TextUtils.isEmpty(charSequence));

        Subscription userEmailSubscription = Observable.combineLatest(userObservable, emailObservable,
                (user, userEmail) -> validateEmail(userEmail.toString()) && userEmail.toString().equals(user.getEmail()))
                .subscribe(aBoolean -> {

                    if (!aBoolean) {
                        showErrorMessage(mEmailInputLayout, getString(R.string.error_message_nonexistent_email));
                    } else {
                        hideErrorMessage(mEmailInputLayout);
                    }
                }, throwable -> {
                    Timber.e(throwable.getMessage());
                });


        Subscription passwordSubscription = Observable.combineLatest(userObservable, passwordObservable,
                (user, userPassword) -> validatePassword(userPassword.toString()) && userPassword.toString().equals(user.getPassword()))
                .subscribe(aBoolean -> {

                    if (!aBoolean) {
                        showErrorMessage(mPasswordInputLayout, getString(R.string.error_message_nonexistent_password));
                    } else {
                        hideErrorMessage(mPasswordInputLayout);
                    }
                }, throwable -> {
                    Timber.e(throwable.getMessage());
                });



        mCompositeSubscription.add(userEmailSubscription);
        mCompositeSubscription.add(passwordSubscription);

        Subscription fieldValidationSubscription = Observable.combineLatest(userObservable, emailObservable, passwordObservable,
                (user, email, password) -> {
                    boolean isEmailValid = validateEmail(email.toString()) && email.toString().equals(user.getEmail());
                    boolean isPasswordValid = validatePassword(password.toString()) && email.toString().equals(user.getPassword());

                    return isEmailValid && isPasswordValid;
                })
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        enableSignIn();
                    } else {
                        disableSignIn();
                    }

                }, throwable -> {
                    Timber.e(throwable.getMessage());
                });

        mCompositeSubscription.add(fieldValidationSubscription);

        RxView.clicks(mButtonSignUp)
                .subscribe(aVoid -> {
                    startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCompositeSubscription.isUnsubscribed())  {
            mCompositeSubscription.unsubscribe();
        }
    }


    @Override
    public int getLayout() {
        return R.layout.activity_login;
    }

    /**
     * Helper method that displays error message.
     *
     * @param textInputLayout {@link TextInputLayout}
     * @param errorMessage    Error Message
     */
    private void showErrorMessage(TextInputLayout textInputLayout, String errorMessage) {
        if (textInputLayout.getChildCount() == 2)
            textInputLayout.getChildAt(1).setVisibility(View.VISIBLE);

        textInputLayout.setErrorEnabled(true);
        textInputLayout.setError(errorMessage);
    }

    /**
     * Helper method that hides error message.
     *
     * @param textInputLayout {@link TextInputLayout}
     */
    private void hideErrorMessage(TextInputLayout textInputLayout) {
        if (textInputLayout.getChildCount() == 2)
            textInputLayout.getChildAt(1).setVisibility(View.GONE);
        textInputLayout.setErrorEnabled(false);
        textInputLayout.setError(null);
    }

    /**
     * Helper method to enable the sign in button
     */
    private void enableSignIn() {
        mSignInLinearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
        mButtonSignIn.setEnabled(true);
        mButtonSignIn.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        RxView.clicks(mButtonSignIn)
                .subscribe(aVoid -> {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                });
    }

    /**
     * Helper method to disable the sign in button
     */
    private void disableSignIn() {
        mSignInLinearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_400));
        mButtonSignIn.setEnabled(false);
        mButtonSignIn.setTextColor(ContextCompat.getColor(this, R.color.grey_500));
    }
}
