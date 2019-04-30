package br.org.cesar.knot_setup_app.activity.configureDevice;
import android.database.Cursor;
import android.util.Log;

import java.util.UUID;

import br.org.cesar.knot_setup_app.activity.configureDevice.ConfigureDeviceContract.Presenter;
import br.org.cesar.knot_setup_app.activity.configureDevice.ConfigureDeviceContract.ViewModel;
import br.org.cesar.knot_setup_app.domain.callback.DeviceCallback;
import br.org.cesar.knot_setup_app.KnotSetupApplication;
import br.org.cesar.knot_setup_app.model.BluetoothDevice;
import br.org.cesar.knot_setup_app.model.Gateway;
import br.org.cesar.knot_setup_app.model.Thing;
import br.org.cesar.knot_setup_app.persistence.mysqlDatabase.DBHelper;
import br.org.cesar.knot_setup_app.wrapper.BluetoothWrapper;
import br.org.cesar.knot_setup_app.utils.Constants;

public class ConfigureDevicePresenter implements Presenter{
    private ViewModel mViewModel;
    private int gatewayID;
    private boolean operation;
    private BluetoothWrapper bluetoothWrapper;
    private BluetoothDevice device;

    private DBHelper mydb;

    private Gateway gateway;
    private Thing thing;

    private Integer read_count = 0;
    private Integer write_count = 0;
    private boolean readDone = false;
    private boolean writeDone = false;


    ConfigureDevicePresenter(ViewModel viewModel,int gatewayID, boolean operation, DBHelper dbHelper){
        this.mViewModel = viewModel;
        this.gatewayID = gatewayID;
        this.operation = operation;
        this.bluetoothWrapper = KnotSetupApplication.getBluetoothWrapper();
        this.device = KnotSetupApplication.getBluetoothDevice();
        this.mydb = dbHelper;
        callbackFlux();
    }

    private void callbackFlux(){

        Log.d("DEV-LOG","CallbackFlux");
        bluetoothWrapper.waitForBonding(device, new DeviceCallback() {
            @Override
            public void onConnect() {
                mViewModel.callbackOnConnected();
                Log.d("DEV-LOG","OnConnect");
                bluetoothWrapper.discoverServices();
            }

            @Override
            public void onCharacteristicChanged(){
                Log.d("DEV-LOG","Characteristic changed?");
            }

            @Override
            public void onDisconnect(){
                Log.d("DEV-LOG","Disconnected");
                bluetoothWrapper.closeGatt();
                mViewModel.callbackOnDisconnected();
            }

            @Override
            public void onServiceDiscoveryComplete(){
                Log.d("DEV-LOG","Services discovered");
                if(operation){
                    createThing();
                    thingConfigWrite();
                }
                else{
                    gateway = new Gateway();
                    gateway.setName(device.getDevice().getName());
                    gatewayConfigRead();
                }
            }

            @Override
            public void onServiceDiscoveryFail(){
                Log.d("DEV-LOG","Service discovery failed");
            }

            @Override
            public void onCharacteristicWriteComplete(){
                Log.d("DEV-LOG","Characteristic writen");
                if(writeDone){
                    thingGatewayWrapper();
                    bluetoothWrapper.closeConn();
                }
                else {
                    write_count++;
                    thingConfigWrite();
                }
            }

            @Override
            public void onCharacteristicWriteFail(){
                Log.d("DEV-LOG","Characteristic write failed");
            }

            @Override
            public void onReadRssiComplete(int rssi){
                Log.d("DEV-LOG","Rssi read: " + rssi);
            }

            @Override
            public void onReadRssiFail(){
                Log.d("DEV-LOG","Rssi read failed");
            }

            @Override
            public void onCharacteristicReadComplete(byte[] value){
                String valueRead;

                if(value[0] < 97){valueRead = bytesToHex(value);}

                else {valueRead = new String(value);}

                Log.d("DEV-LOG","Characteristic read: " + valueRead);

                // Add read characteristic to gateway object
                gatewayConfigPersist(new String(value));

                if(readDone){
                    gatewayDBWrapper();
                    bluetoothWrapper.closeConn();
                }

                else {
                    read_count++;
                    gatewayConfigRead();
                }
            }

            @Override
            public void onCharacteristicReadFail(){
                Log.d("DEV-LOG","Characteristic read failed");
            }

        });
    }



    private void writeWrapper(UUID service, UUID characteristic, String valtoWrite){
        this.bluetoothWrapper.write(service,characteristic,valtoWrite);
    }


    private void writeWrapper(UUID service, UUID characteristic, byte[] valtoWrite){
        this.bluetoothWrapper.write(service,characteristic,valtoWrite);
    }

    private void readWrapper(UUID service, UUID characteristic){
        this.bluetoothWrapper.readCharacteristic(service,characteristic);
    }

    private void thingConfigWrite(){
        byte[] value = new byte[1];
        value[0] = (byte) (0x12);

        switch (write_count){
            case 0:
                Log.d("DEV-LOG", "Write Wrapper: Channel" );
                writeWrapper(Constants.OT_SETTINGS_SERVICE,Constants.CHANNEL_CHARACTERISTIC,value);
                break;
            case 1:
                Log.d("DEV-LOG", "WriteWrapper: NetName");
                writeWrapper(Constants.OT_SETTINGS_SERVICE,Constants.NET_NAME_CHARACTERISTIC,thing.getNetName());
                break;
            case 2:
                Log.d("DEV-LOG", "WriteWrapper: PanID");
                writeWrapper(Constants.OT_SETTINGS_SERVICE,Constants.PAN_ID_CHARACTERISTIC,value);
                break;
            case 3:
                Log.d("DEV-LOG", "WriteWrapper: XpanID");
                writeWrapper(Constants.OT_SETTINGS_SERVICE,Constants.XPANID_CHARACTERISTIC,thing.getXpanID());
                break;
            case 4:
                Log.d("DEV-LOG", "WriteWrapper: IPV6");
                writeWrapper(Constants.IPV6_SERVICE,Constants.IPV6_CHARACTERISTIC,thing.getPanID());
                writeDone = true;
        }
    }

    private void gatewayConfigRead(){
        switch (read_count){
            case 0:
                Log.d("DEV-LOG", "ReadWrapper: Channel" );
                readWrapper(Constants.OT_SETTINGS_SERVICE_GATEWAY,Constants.CHANNEL_CHARACTERISTIC_GATEWAY);
                break;
            case 1:
                Log.d("DEV-LOG", "ReadWrapper: NetName");
                readWrapper(Constants.OT_SETTINGS_SERVICE_GATEWAY,Constants.NET_NAME_CHARACTERISTIC_GATEWAY);
                break;
            case 2:
                Log.d("DEV-LOG", "ReadWrapper: PanID");
                readWrapper(Constants.OT_SETTINGS_SERVICE_GATEWAY,Constants.PAN_ID_CHARACTERISTIC_GATEWAY);
                break;
            case 3:
                Log.d("DEV-LOG", "ReadWrapper: XpanID");
                readWrapper(Constants.OT_SETTINGS_SERVICE_GATEWAY,Constants.XPANID_CHARACTERISTIC_GATEWAY);
                break;
            case 4:
                Log.d("DEV-LOG", "ReadWrapper: IPV6");
                readWrapper(Constants.OT_SETTINGS_SERVICE_GATEWAY,Constants.IPV6_CHARACTERISTIC_GATEWAY);
                readDone = true;
        }
    }

    private void gatewayConfigPersist(String value){
        switch (read_count){
            case 0:
                gateway.setChannel(value);
                break;
            case 1:
                gateway.setNetName(value);
                break;
            case 2:
                gateway.setPanID(value);
                break;
            case 3:
                gateway.setXpanID(value);
                break;
            case 4:
                gateway.setIpv6(value);
        }
    }

    private static String bytesToHex(byte[] hashInBytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void gatewayDBWrapper(){
        Log.d("DEV-LOG","Writing to database");
        mydb.insertDevice(gateway.getID(),gateway.getName(),
                gateway.getChannel(),gateway.getNetName(),
                gateway.getPanID(),gateway.getXpanID(),
                gateway.getMasterkey(),gateway.getIpv6());
        Log.d("DEV-LOG","Writing to database over");
    }

    private void thingGatewayWrapper(){
        Log.d("DEV-LOG","Writing to database");
        mydb.insertThing(thing.getID(),thing.getNickname(),thing.getChannel(),thing.getNickname(),thing.getPanID(),thing.getXpanID(),thing.getMasterkey(),thing.getIpv6());
        mydb.insertGatewayThing(gatewayID,thing.getID());
        Log.d("DEV-LOG","Writing to database over");
    }

    //TODO: Find out if I really need to set te Thing values
    private void createThing(){
        Log.d("DEV-LOG","onCreateThing " + gatewayID);
        Cursor configs = mydb.getData("id",gatewayID);
        thing = new Thing(123123213,device.getDevice().getName(),
                configs.getString(configs.getColumnIndex("Channel")),
                configs.getString(configs.getColumnIndex("NetName")),
                configs.getString(configs.getColumnIndex("PanID")),
                configs.getString(configs.getColumnIndex("XpanID")),
                configs.getString(configs.getColumnIndex("Masterkey")),
                configs.getString(configs.getColumnIndex("IPV6")) );
                thing.printThingSettings();
    }
}