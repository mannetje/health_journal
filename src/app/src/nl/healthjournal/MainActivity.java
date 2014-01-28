package nl.healthjournal;


import org.w3c.dom.Text;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity 
extends Activity 
implements OnClickListener{
	
	private Button ok_Button;
	private TextView myText;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ok_Button = (Button) this.findViewById(R.id.okButton);
         myText = (TextView) this.findViewById(R.id.editText1);
        ok_Button.setOnClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


	@Override
	public void onClick(View v) {
		myText.setText("Health Logger test");
		
	}
    
}
