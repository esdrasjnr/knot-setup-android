package br.org.cesar.knot_setup_app.view.splash;

public interface SplashContract {

    interface Presenter {
        void chooseActivity();
    }

    interface ViewModel{
        void doLogin();
        void doListGateways();
        void apiNotConfigured();
        void callbackOnConnectionError();
    }
}
