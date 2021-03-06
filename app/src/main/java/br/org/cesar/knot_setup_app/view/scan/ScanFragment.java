package br.org.cesar.knot_setup_app.view.scan;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.org.cesar.knot_setup_app.R;
import br.org.cesar.knot_setup_app.view.ConfigureDeviceActivity;
import br.org.cesar.knot_setup_app.view.ConfigureGatewayWifiActivity;
import br.org.cesar.knot_setup_app.domain.adapter.ScanAdapter;
import br.org.cesar.knot_setup_app.view.scan.ScanContract.ViewModel;
import br.org.cesar.knot_setup_app.view.scan.ScanContract.Presenter;
import br.org.cesar.knot_setup_app.model.BluetoothDevice;
import br.org.cesar.knot_setup_app.utils.Constants;
import br.org.cesar.knot_setup_app.wrapper.LogWrapper;

import static br.org.cesar.knot_setup_app.utils.Constants.GATEWAY_NAME;
import static br.org.cesar.knot_setup_app.utils.Constants.THING_NAME;

public class ScanFragment extends Fragment implements ViewModel {

    private ListView deviceListView;
    private TextView feedbackMessage;

    private ScanAdapter adapter;
    private Presenter presenter;
    private List<BluetoothDevice> deviceList;
    private UUID service;
    private Handler handler;
    private Context context;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getContext().getApplicationContext();
        presenter = new ScanPresenter(this, service, context);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container,false);

        deviceList = new ArrayList<>();
        adapter = new ScanAdapter(getActivity(), R.layout.item_device, deviceList);
        deviceListView = view.findViewById(R.id.list);
        feedbackMessage = view.findViewById(R.id.feedback_message);
        handler = new Handler(Looper.getMainLooper());

        setupAdapter();

        return view;
    }



    /**
     * Setup device list adapter
     *
     */
    private void setupAdapter() {
        //Define list view and adapter
        deviceListView.setAdapter(adapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //When device is clicked we must connect to it
                final BluetoothDevice device = deviceList.get(position);
                presenter.onDeviceSelected(device);
            }
        });
    }

    @Override
    public void onGatewayWifiConfiguration(String gatewayName) {
        Intent intent = new Intent(context, ConfigureGatewayWifiActivity.class);
        intent.putExtra(GATEWAY_NAME, gatewayName);
        startActivity(intent);
    }

    @Override
    public void onThingSelected(String thingName) {
        Intent intent = new Intent(context, ConfigureDeviceActivity.class);
        intent.putExtra(THING_NAME, thingName);
        startActivity(intent);
    }

    @Override
    public void setBluetoothFeedback(int visibility) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                feedbackMessage.setText(getString(R.string.scan_turn_bt_on));
                feedbackMessage.setVisibility(visibility);
            }
        });
    }

    @Override
    public void onDeviceFound(List<BluetoothDevice> deviceList) {
        this.deviceList.clear();
        this.deviceList.addAll(deviceList);
        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onScanFail() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, getString(R.string.all_scan_failed), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onBluetoothPermissionRequired() {
        LogWrapper.Log("callbackOnBluetoothPermissionRequired", Log.DEBUG);
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        this.startActivity(intent);
        if(getActivity() != null){
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.BLUETOOTH_PERMISSION_ID);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        presenter.onFocus();
    }

    @Override
    public void onStop() {
        super.onStop();
        LogWrapper.Log("ScanFragment onFocusLost",Log.DEBUG);
        presenter.onFocusLost();
    }

    public void setService(UUID service) {
        this.service = service;
    }

}
