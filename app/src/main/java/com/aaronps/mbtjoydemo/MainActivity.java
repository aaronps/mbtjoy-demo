package com.aaronps.mbtjoydemo;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.aaronps.bluetoothtank.BluetoothTank;
import com.aaronps.touchjoystick.SplitJoystick;

public class MainActivity extends AppCompatActivity implements SplitJoystick.Listener, BluetoothTank.Listener {

    private static final String TAG = "Main";

    static final int MSG_CONNECT_UPDATE = 0;

    static final int STATE_DISCONNECTED = 0;
    static final int STATE_CONNECTED = 1;

    static final int MOVESTATE_STOP = 0;
    static final int MOVESTATE_FORWARD = 1;
    static final int MOVESTATE_TURN = 2;

    TextView mConnectionStatus;
    TextView mVerticalValue;
    TextView mHorizontalValue;

    SplitJoystick mTouchJoystick;
    BluetoothTank mbtShell;
    Handler mHandler;

    int mConnectState = STATE_DISCONNECTED;
    int mForwardSpeed = 0;
    int mTurnSpeed = 0;
    int mMoveState = MOVESTATE_STOP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConnectionStatus = (TextView) findViewById(R.id.textview_connection_status);
        mVerticalValue = (TextView) findViewById(R.id.textview_vertical_value);
        mHorizontalValue = (TextView) findViewById(R.id.textview_horizontal_value);

        mTouchJoystick = new SplitJoystick(this, this);
        mbtShell = new BluetoothTank("HC-06", this);

        mHandler = new Handler(Looper.getMainLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch ( msg.what )
                {
                    case MSG_CONNECT_UPDATE:
                        switch (msg.arg1)
                        {
                            case STATE_CONNECTED:
                                mConnectState = STATE_CONNECTED;
                                mConnectionStatus.setText("Connected!!!");
                                break;

                            case STATE_DISCONNECTED:
                                mConnectState = STATE_DISCONNECTED;
                                mConnectionStatus.setText("Disconnected. Connecting...");
                                break;
                        }
                        break;

                    default:
                        super.handleMessage(msg);
                }
            }
        };
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mbtShell.start();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        try
        {
            mbtShell.stop();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    void setNewForwardSpeed(int newSpeed)
    {
        if (newSpeed != mForwardSpeed)
        {
            if (mMoveState != MOVESTATE_TURN)
            {
                if ( newSpeed > 0 )
                {
                    mbtShell.commandSetSpeed(newSpeed);
                    mbtShell.commandUp();
                    mMoveState = MOVESTATE_FORWARD;
                }
                else if ( newSpeed < 0 )
                {
                    mbtShell.commandSetSpeed(-newSpeed);
                    mbtShell.commandDown();
                    mMoveState = MOVESTATE_FORWARD;
                }
                else
                {
                    mbtShell.commandStop();
                    mMoveState = MOVESTATE_STOP;
                }
            }
            mForwardSpeed = newSpeed;
        }
    }

    void setNewTurnSpeed(int newSpeed)
    {
        if (newSpeed != mTurnSpeed)
        {
            if (mMoveState != MOVESTATE_FORWARD)
            {
                if ( newSpeed > 0 )
                {
                    mbtShell.commandSetSpeed(newSpeed);
                    mbtShell.commandRight();
                    mMoveState = MOVESTATE_TURN;
                }
                else if ( newSpeed < 0 )
                {
                    mbtShell.commandSetSpeed(-newSpeed);
                    mbtShell.commandLeft();
                    mMoveState = MOVESTATE_TURN;
                }
                else
                {
                    mbtShell.commandStop();
                    mMoveState = MOVESTATE_STOP;
                }
            }
            mTurnSpeed = newSpeed;
        }
    }

    @Override
    public void onVerticalJoyChange(float value)
    {
        final float absvalue = Math.abs(value);

        int speedValue = 0;

        if (absvalue >= 0.80f) speedValue = 3;
        else if (absvalue >= 0.5f) speedValue = 2;
        else if (absvalue >= 0.15f) speedValue = 1;

        if (value < 0.0f) speedValue = -speedValue;

        if (speedValue != mForwardSpeed)
        {
            if ( speedValue != 0 )
            {
                setNewForwardSpeed(speedValue);
            }
            else
            {
                if ( mTurnSpeed != 0 )
                {
                    mForwardSpeed = 0;
                    mMoveState = MOVESTATE_STOP;
                    int t = mTurnSpeed;
                    mTurnSpeed = 0;
                    setNewTurnSpeed(t);
                }
                else
                {
                    setNewForwardSpeed(0); // stop
                }
            }
        }
    }

    @Override
    public void onHorizontalJoyChange(float value)
    {
        final float absvalue = Math.abs(value);

        int speedValue = 0;

        if (absvalue >= 0.95f) speedValue = 3;
        else if (absvalue >= 0.7f) speedValue = 2;
        else if (absvalue >= 0.15f) speedValue = 1;

        if (value < 0.0f) speedValue = -speedValue;

        if ( speedValue != mTurnSpeed )
        {
            if ( speedValue != 0)
            {
                setNewTurnSpeed(speedValue);
            }
            else
            {
                if (mForwardSpeed != 0)
                {
                    mTurnSpeed = 0;
                    mMoveState = MOVESTATE_STOP;
                    int t = mForwardSpeed;
                    mForwardSpeed = 0;
                    setNewForwardSpeed(t);
                }
                else
                {
                    setNewTurnSpeed(0);
                }
            }
        }
    }

    @Override
    public void onBluetoothTankConnected(BluetoothTank shell)
    {
        mHandler.obtainMessage(MSG_CONNECT_UPDATE, STATE_CONNECTED, 0).sendToTarget();
    }

    @Override
    public void onBluetoothTankDisconnected(BluetoothTank shell)
    {
        mHandler.obtainMessage(MSG_CONNECT_UPDATE, STATE_DISCONNECTED, 0).sendToTarget();
    }

}
