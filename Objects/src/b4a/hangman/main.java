package b4a.hangman;

import anywheresoftware.b4a.B4AMenuItem;
import android.app.Activity;
import android.os.Bundle;
import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.BALayout;
import anywheresoftware.b4a.B4AActivity;
import anywheresoftware.b4a.ObjectWrapper;
import anywheresoftware.b4a.objects.ActivityWrapper;
import java.lang.reflect.InvocationTargetException;
import anywheresoftware.b4a.B4AUncaughtException;
import anywheresoftware.b4a.debug.*;
import java.lang.ref.WeakReference;

public class main extends Activity implements B4AActivity{
	public static main mostCurrent;
	static boolean afterFirstLayout;
	static boolean isFirst = true;
    private static boolean processGlobalsRun = false;
	BALayout layout;
	public static BA processBA;
	BA activityBA;
    ActivityWrapper _activity;
    java.util.ArrayList<B4AMenuItem> menuItems;
	public static final boolean fullScreen = true;
	public static final boolean includeTitle = false;
    public static WeakReference<Activity> previousOne;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFirst) {
			processBA = new BA(this.getApplicationContext(), null, null, "b4a.hangman", "b4a.hangman.main");
			processBA.loadHtSubs(this.getClass());
	        float deviceScale = getApplicationContext().getResources().getDisplayMetrics().density;
	        BALayout.setDeviceScale(deviceScale);
            
		}
		else if (previousOne != null) {
			Activity p = previousOne.get();
			if (p != null && p != this) {
                BA.LogInfo("Killing previous instance (main).");
				p.finish();
			}
		}
		if (!includeTitle) {
        	this.getWindow().requestFeature(android.view.Window.FEATURE_NO_TITLE);
        }
        if (fullScreen) {
        	getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,   
        			android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
		mostCurrent = this;
        processBA.sharedProcessBA.activityBA = null;
		layout = new BALayout(this);
		setContentView(layout);
		afterFirstLayout = false;
		BA.handler.postDelayed(new WaitForLayout(), 5);

	}
	private static class WaitForLayout implements Runnable {
		public void run() {
			if (afterFirstLayout)
				return;
			if (mostCurrent == null)
				return;
            
			if (mostCurrent.layout.getWidth() == 0) {
				BA.handler.postDelayed(this, 5);
				return;
			}
			mostCurrent.layout.getLayoutParams().height = mostCurrent.layout.getHeight();
			mostCurrent.layout.getLayoutParams().width = mostCurrent.layout.getWidth();
			afterFirstLayout = true;
			mostCurrent.afterFirstLayout();
		}
	}
	private void afterFirstLayout() {
        if (this != mostCurrent)
			return;
		activityBA = new BA(this, layout, processBA, "b4a.hangman", "b4a.hangman.main");
        
        processBA.sharedProcessBA.activityBA = new java.lang.ref.WeakReference<BA>(activityBA);
        anywheresoftware.b4a.objects.ViewWrapper.lastId = 0;
        _activity = new ActivityWrapper(activityBA, "activity");
        anywheresoftware.b4a.Msgbox.isDismissing = false;
        if (BA.isShellModeRuntimeCheck(processBA)) {
			if (isFirst)
				processBA.raiseEvent2(null, true, "SHELL", false);
			processBA.raiseEvent2(null, true, "CREATE", true, "b4a.hangman.main", processBA, activityBA, _activity, anywheresoftware.b4a.keywords.Common.Density);
			_activity.reinitializeForShell(activityBA, "activity");
		}
        initializeProcessGlobals();		
        initializeGlobals();
        
        BA.LogInfo("** Activity (main) Create, isFirst = " + isFirst + " **");
        processBA.raiseEvent2(null, true, "activity_create", false, isFirst);
		isFirst = false;
		if (this != mostCurrent)
			return;
        processBA.setActivityPaused(false);
        BA.LogInfo("** Activity (main) Resume **");
        processBA.raiseEvent(null, "activity_resume");
        if (android.os.Build.VERSION.SDK_INT >= 11) {
			try {
				android.app.Activity.class.getMethod("invalidateOptionsMenu").invoke(this,(Object[]) null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	public void addMenuItem(B4AMenuItem item) {
		if (menuItems == null)
			menuItems = new java.util.ArrayList<B4AMenuItem>();
		menuItems.add(item);
	}
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (menuItems == null)
			return false;
		for (B4AMenuItem bmi : menuItems) {
			android.view.MenuItem mi = menu.add(bmi.title);
			if (bmi.drawable != null)
				mi.setIcon(bmi.drawable);
            if (android.os.Build.VERSION.SDK_INT >= 11) {
				try {
                    if (bmi.addToBar) {
				        android.view.MenuItem.class.getMethod("setShowAsAction", int.class).invoke(mi, 1);
                    }
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			mi.setOnMenuItemClickListener(new B4AMenuItemsClickListener(bmi.eventName.toLowerCase(BA.cul)));
		}
		return true;
	}
    public void onWindowFocusChanged(boolean hasFocus) {
       super.onWindowFocusChanged(hasFocus);
       if (processBA.subExists("activity_windowfocuschanged"))
           processBA.raiseEvent2(null, true, "activity_windowfocuschanged", false, hasFocus);
    }
	private class B4AMenuItemsClickListener implements android.view.MenuItem.OnMenuItemClickListener {
		private final String eventName;
		public B4AMenuItemsClickListener(String eventName) {
			this.eventName = eventName;
		}
		public boolean onMenuItemClick(android.view.MenuItem item) {
			processBA.raiseEvent(item.getTitle(), eventName + "_click");
			return true;
		}
	}
    public static Class<?> getObject() {
		return main.class;
	}
    private Boolean onKeySubExist = null;
    private Boolean onKeyUpSubExist = null;
	@Override
	public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
		if (onKeySubExist == null)
			onKeySubExist = processBA.subExists("activity_keypress");
		if (onKeySubExist) {
			if (keyCode == anywheresoftware.b4a.keywords.constants.KeyCodes.KEYCODE_BACK &&
					android.os.Build.VERSION.SDK_INT >= 18) {
				HandleKeyDelayed hk = new HandleKeyDelayed();
				hk.kc = keyCode;
				BA.handler.post(hk);
				return true;
			}
			else {
				boolean res = new HandleKeyDelayed().runDirectly(keyCode);
				if (res)
					return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	private class HandleKeyDelayed implements Runnable {
		int kc;
		public void run() {
			runDirectly(kc);
		}
		public boolean runDirectly(int keyCode) {
			Boolean res =  (Boolean)processBA.raiseEvent2(_activity, false, "activity_keypress", false, keyCode);
			if (res == null || res == true) {
                return true;
            }
            else if (keyCode == anywheresoftware.b4a.keywords.constants.KeyCodes.KEYCODE_BACK) {
				finish();
				return true;
			}
            return false;
		}
		
	}
    @Override
	public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
		if (onKeyUpSubExist == null)
			onKeyUpSubExist = processBA.subExists("activity_keyup");
		if (onKeyUpSubExist) {
			Boolean res =  (Boolean)processBA.raiseEvent2(_activity, false, "activity_keyup", false, keyCode);
			if (res == null || res == true)
				return true;
		}
		return super.onKeyUp(keyCode, event);
	}
	@Override
	public void onNewIntent(android.content.Intent intent) {
		this.setIntent(intent);
	}
    @Override 
	public void onPause() {
		super.onPause();
        if (_activity == null) //workaround for emulator bug (Issue 2423)
            return;
		anywheresoftware.b4a.Msgbox.dismiss(true);
        BA.LogInfo("** Activity (main) Pause, UserClosed = " + activityBA.activity.isFinishing() + " **");
        processBA.raiseEvent2(_activity, true, "activity_pause", false, activityBA.activity.isFinishing());		
        processBA.setActivityPaused(true);
        mostCurrent = null;
        if (!activityBA.activity.isFinishing())
			previousOne = new WeakReference<Activity>(this);
        anywheresoftware.b4a.Msgbox.isDismissing = false;
	}

	@Override
	public void onDestroy() {
        super.onDestroy();
		previousOne = null;
	}
    @Override 
	public void onResume() {
		super.onResume();
        mostCurrent = this;
        anywheresoftware.b4a.Msgbox.isDismissing = false;
        if (activityBA != null) { //will be null during activity create (which waits for AfterLayout).
        	ResumeMessage rm = new ResumeMessage(mostCurrent);
        	BA.handler.post(rm);
        }
	}
    private static class ResumeMessage implements Runnable {
    	private final WeakReference<Activity> activity;
    	public ResumeMessage(Activity activity) {
    		this.activity = new WeakReference<Activity>(activity);
    	}
		public void run() {
			if (mostCurrent == null || mostCurrent != activity.get())
				return;
			processBA.setActivityPaused(false);
            BA.LogInfo("** Activity (main) Resume **");
		    processBA.raiseEvent(mostCurrent._activity, "activity_resume", (Object[])null);
		}
    }
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
	      android.content.Intent data) {
		processBA.onActivityResult(requestCode, resultCode, data);
	}
	private static void initializeGlobals() {
		processBA.raiseEvent2(null, true, "globals", false, (Object[])null);
	}

public anywheresoftware.b4a.keywords.Common __c = null;
public anywheresoftware.b4a.objects.ButtonWrapper[][] _buttons = null;
public anywheresoftware.b4a.objects.collections.List _wordlist = null;
public static int _counter = 0;
public static String[] _character = null;
public static int _k = 0;
public static int _i = 0;
public static int _j = 0;
public static int _generate = 0;
public anywheresoftware.b4a.objects.LabelWrapper _label1 = null;
public anywheresoftware.b4a.objects.LabelWrapper _label2 = null;
public anywheresoftware.b4a.objects.LabelWrapper _label3 = null;
public anywheresoftware.b4a.objects.LabelWrapper _label4 = null;
public anywheresoftware.b4a.objects.LabelWrapper _label5 = null;
public anywheresoftware.b4a.objects.LabelWrapper _label6 = null;
public anywheresoftware.b4a.objects.LabelWrapper _label10 = null;
public anywheresoftware.b4a.objects.LabelWrapper _label7 = null;
public anywheresoftware.b4a.objects.LabelWrapper _label8 = null;
public anywheresoftware.b4a.objects.LabelWrapper _label9 = null;
public static String _wordpick = "";
public anywheresoftware.b4a.objects.LabelWrapper _letters_lbl = null;
public anywheresoftware.b4a.objects.LabelWrapper _counter_lbl = null;
public anywheresoftware.b4a.objects.LabelWrapper _guess_lbl = null;
public anywheresoftware.b4a.objects.LabelWrapper _label11 = null;
public anywheresoftware.b4a.objects.PanelWrapper _panel1 = null;
public anywheresoftware.b4a.objects.PanelWrapper _panel2 = null;

public static boolean isAnyActivityVisible() {
    boolean vis = false;
vis = vis | (main.mostCurrent != null);
return vis;}
public static String  _about_click() throws Exception{
 //BA.debugLineNum = 138;BA.debugLine="Sub about_Click";
 //BA.debugLineNum = 139;BA.debugLine="Panel2.Visible = True";
mostCurrent._panel2.setVisible(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 140;BA.debugLine="Panel1.Visible = False";
mostCurrent._panel1.setVisible(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 142;BA.debugLine="End Sub";
return "";
}
public static String  _activity_create(boolean _firsttime) throws Exception{
int _width = 0;
int _offsetx = 0;
int _offsety = 0;
anywheresoftware.b4a.objects.ButtonWrapper _b = null;
 //BA.debugLineNum = 57;BA.debugLine="Sub Activity_Create(FirstTime As Boolean)";
 //BA.debugLineNum = 59;BA.debugLine="Activity.LoadLayout(\"Main\")";
mostCurrent._activity.LoadLayout("Main",mostCurrent.activityBA);
 //BA.debugLineNum = 61;BA.debugLine="Panel1.SetLayout(0,0,100%x,100%y)";
mostCurrent._panel1.SetLayout((int) (0),(int) (0),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (100),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (100),mostCurrent.activityBA));
 //BA.debugLineNum = 62;BA.debugLine="Panel2.SetLayout(0,0,100%x,100%y)";
mostCurrent._panel2.SetLayout((int) (0),(int) (0),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (100),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (100),mostCurrent.activityBA));
 //BA.debugLineNum = 63;BA.debugLine="Panel2.Visible= False";
mostCurrent._panel2.setVisible(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 66;BA.debugLine="counter_lbl.SetLayout(5%x,5%y,30%x,17%y)";
mostCurrent._counter_lbl.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (30),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (17),mostCurrent.activityBA));
 //BA.debugLineNum = 67;BA.debugLine="letters_lbl.SetLayout(60%x,5%y,30%x,17%y)";
mostCurrent._letters_lbl.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (60),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (30),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (17),mostCurrent.activityBA));
 //BA.debugLineNum = 69;BA.debugLine="guess_lbl.SetLayout(5%x,20%y,35%x,20%y)";
mostCurrent._guess_lbl.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (35),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA));
 //BA.debugLineNum = 70;BA.debugLine="guess_lbl.TextSize = 25";
mostCurrent._guess_lbl.setTextSize((float) (25));
 //BA.debugLineNum = 71;BA.debugLine="guess_lbl.Text=\"Your Guess: \"";
mostCurrent._guess_lbl.setText((Object)("Your Guess: "));
 //BA.debugLineNum = 72;BA.debugLine="Label1.SetLayout(40%x,20%y,5%x,20%y)";
mostCurrent._label1.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (40),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA));
 //BA.debugLineNum = 73;BA.debugLine="Label1.TextSize = 25";
mostCurrent._label1.setTextSize((float) (25));
 //BA.debugLineNum = 74;BA.debugLine="Label2.SetLayout(45%x,20%y,5%x,20%y)";
mostCurrent._label2.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (45),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA));
 //BA.debugLineNum = 75;BA.debugLine="Label2.TextSize = 25";
mostCurrent._label2.setTextSize((float) (25));
 //BA.debugLineNum = 76;BA.debugLine="Label3.SetLayout(50%x,20%y,5%x,20%y)";
mostCurrent._label3.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (50),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA));
 //BA.debugLineNum = 77;BA.debugLine="Label3.TextSize = 25";
mostCurrent._label3.setTextSize((float) (25));
 //BA.debugLineNum = 78;BA.debugLine="Label4.SetLayout(55%x,20%y,5%x,20%y)";
mostCurrent._label4.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (55),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA));
 //BA.debugLineNum = 79;BA.debugLine="Label4.TextSize = 25";
mostCurrent._label4.setTextSize((float) (25));
 //BA.debugLineNum = 80;BA.debugLine="Label5.SetLayout(60%x,20%y,5%x,20%y)";
mostCurrent._label5.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (60),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA));
 //BA.debugLineNum = 81;BA.debugLine="Label5.TextSize = 25";
mostCurrent._label5.setTextSize((float) (25));
 //BA.debugLineNum = 82;BA.debugLine="Label6.SetLayout(65%x,20%y,5%x,20%y)";
mostCurrent._label6.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (65),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA));
 //BA.debugLineNum = 83;BA.debugLine="Label6.TextSize = 25";
mostCurrent._label6.setTextSize((float) (25));
 //BA.debugLineNum = 84;BA.debugLine="Label7.SetLayout(70%x,20%y,5%x,20%y)";
mostCurrent._label7.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (70),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA));
 //BA.debugLineNum = 85;BA.debugLine="Label7.TextSize = 25";
mostCurrent._label7.setTextSize((float) (25));
 //BA.debugLineNum = 86;BA.debugLine="Label8.SetLayout(75%x,20%y,5%x,20%y)";
mostCurrent._label8.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (75),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA));
 //BA.debugLineNum = 87;BA.debugLine="Label8.TextSize = 25";
mostCurrent._label8.setTextSize((float) (25));
 //BA.debugLineNum = 88;BA.debugLine="Label9.SetLayout(80%x,20%y,5%x,20%y)";
mostCurrent._label9.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (80),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA));
 //BA.debugLineNum = 89;BA.debugLine="Label9.TextSize = 25";
mostCurrent._label9.setTextSize((float) (25));
 //BA.debugLineNum = 90;BA.debugLine="Label10.SetLayout(85%x,20%y,5%x,20%y)";
mostCurrent._label10.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (85),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA));
 //BA.debugLineNum = 91;BA.debugLine="Label10.TextSize = 25";
mostCurrent._label10.setTextSize((float) (25));
 //BA.debugLineNum = 92;BA.debugLine="Label11.SetLayout(90%x,20%y,5%x,20%y)";
mostCurrent._label11.SetLayout(anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (90),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (5),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (20),mostCurrent.activityBA));
 //BA.debugLineNum = 93;BA.debugLine="Label11.TextSize = 25";
mostCurrent._label11.setTextSize((float) (25));
 //BA.debugLineNum = 95;BA.debugLine="Activity.AddMenuItem(\"Restart\",\"restart\")";
mostCurrent._activity.AddMenuItem("Restart","restart");
 //BA.debugLineNum = 96;BA.debugLine="Activity.AddMenuItem(\"About\",\"about\")";
mostCurrent._activity.AddMenuItem("About","about");
 //BA.debugLineNum = 100;BA.debugLine="Dim width, offsetX, offsetY As Int";
_width = 0;
_offsetx = 0;
_offsety = 0;
 //BA.debugLineNum = 101;BA.debugLine="width = 50dip";
_width = anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (50));
 //BA.debugLineNum = 102;BA.debugLine="offsetX = (100%x - width * 10 - 2dip * 9) / 2";
_offsetx = (int) ((anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (100),mostCurrent.activityBA)-_width*10-anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (2))*9)/(double)2);
 //BA.debugLineNum = 103;BA.debugLine="offsetY = (100%y - width * 3 - 2dip * 2) - 5dip";
_offsety = (int) ((anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (100),mostCurrent.activityBA)-_width*3-anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (2))*2)-anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (5)));
 //BA.debugLineNum = 106;BA.debugLine="For i = 0 To 8";
{
final int step67 = 1;
final int limit67 = (int) (8);
for (_i = (int) (0); (step67 > 0 && _i <= limit67) || (step67 < 0 && _i >= limit67); _i = ((int)(0 + _i + step67))) {
 //BA.debugLineNum = 107;BA.debugLine="For j = 0 To 2";
{
final int step68 = 1;
final int limit68 = (int) (2);
for (_j = (int) (0); (step68 > 0 && _j <= limit68) || (step68 < 0 && _j >= limit68); _j = ((int)(0 + _j + step68))) {
 //BA.debugLineNum = 108;BA.debugLine="Dim b As Button";
_b = new anywheresoftware.b4a.objects.ButtonWrapper();
 //BA.debugLineNum = 109;BA.debugLine="b.Initialize(\"buttons\") 'All buttons share the same event sub";
_b.Initialize(mostCurrent.activityBA,"buttons");
 //BA.debugLineNum = 110;BA.debugLine="b.TextSize = 25";
_b.setTextSize((float) (25));
 //BA.debugLineNum = 112;BA.debugLine="Activity.AddView(b,offsetX + i * (width + 2dip), offsetY + j * (width + 2dip), width, width)";
mostCurrent._activity.AddView((android.view.View)(_b.getObject()),(int) (_offsetx+_i*(_width+anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (2)))),(int) (_offsety+_j*(_width+anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (2)))),_width,_width);
 //BA.debugLineNum = 113;BA.debugLine="buttons(i, j) = b 'store a reference to this view";
mostCurrent._buttons[_i][_j] = _b;
 }
};
 }
};
 //BA.debugLineNum = 118;BA.debugLine="wordList = File.ReadList(File.DirAssets, \"words.txt\")";
mostCurrent._wordlist = anywheresoftware.b4a.keywords.Common.File.ReadList(anywheresoftware.b4a.keywords.Common.File.getDirAssets(),"words.txt");
 //BA.debugLineNum = 119;BA.debugLine="generate = Rnd(0,wordList.Size -1)";
_generate = anywheresoftware.b4a.keywords.Common.Rnd((int) (0),(int) (mostCurrent._wordlist.getSize()-1));
 //BA.debugLineNum = 120;BA.debugLine="For i = 0 To 8";
{
final int step78 = 1;
final int limit78 = (int) (8);
for (_i = (int) (0); (step78 > 0 && _i <= limit78) || (step78 < 0 && _i >= limit78); _i = ((int)(0 + _i + step78))) {
 //BA.debugLineNum = 121;BA.debugLine="For j = 0 To 2";
{
final int step79 = 1;
final int limit79 = (int) (2);
for (_j = (int) (0); (step79 > 0 && _j <= limit79) || (step79 < 0 && _j >= limit79); _j = ((int)(0 + _j + step79))) {
 //BA.debugLineNum = 122;BA.debugLine="buttons(i,j).Text = character(k)";
mostCurrent._buttons[_i][_j].setText((Object)(mostCurrent._character[_k]));
 //BA.debugLineNum = 123;BA.debugLine="k = k+1";
_k = (int) (_k+1);
 }
};
 }
};
 //BA.debugLineNum = 128;BA.debugLine="wordpick = wordList.Get(generate)";
mostCurrent._wordpick = BA.ObjectToString(mostCurrent._wordlist.Get(_generate));
 //BA.debugLineNum = 130;BA.debugLine="counter_lbl.text = \"You have: \" & counter &\" Attempts\"";
mostCurrent._counter_lbl.setText((Object)("You have: "+BA.NumberToString(_counter)+" Attempts"));
 //BA.debugLineNum = 131;BA.debugLine="letters_lbl.Text = \"The word has: \" & wordpick.Length &\" Letters\"";
mostCurrent._letters_lbl.setText((Object)("The word has: "+BA.NumberToString(mostCurrent._wordpick.length())+" Letters"));
 //BA.debugLineNum = 136;BA.debugLine="End Sub";
return "";
}
public static String  _activity_pause(boolean _userclosed) throws Exception{
 //BA.debugLineNum = 173;BA.debugLine="Sub Activity_Pause (UserClosed As Boolean)";
 //BA.debugLineNum = 175;BA.debugLine="End Sub";
return "";
}
public static String  _activity_resume() throws Exception{
 //BA.debugLineNum = 169;BA.debugLine="Sub Activity_Resume";
 //BA.debugLineNum = 171;BA.debugLine="End Sub";
return "";
}
public static String  _buttons_click() throws Exception{
anywheresoftware.b4a.objects.ButtonWrapper _b = null;
int _response = 0;
 //BA.debugLineNum = 177;BA.debugLine="Sub buttons_Click";
 //BA.debugLineNum = 179;BA.debugLine="Dim b As Button";
_b = new anywheresoftware.b4a.objects.ButtonWrapper();
 //BA.debugLineNum = 180;BA.debugLine="b = Sender";
_b.setObject((android.widget.Button)(anywheresoftware.b4a.keywords.Common.Sender(mostCurrent.activityBA)));
 //BA.debugLineNum = 182;BA.debugLine="wordpick = wordList.Get(generate)";
mostCurrent._wordpick = BA.ObjectToString(mostCurrent._wordlist.Get(_generate));
 //BA.debugLineNum = 184;BA.debugLine="If  wordpick.Contains( b.Text ) = True  Then";
if (mostCurrent._wordpick.contains(_b.getText())==anywheresoftware.b4a.keywords.Common.True) { 
 //BA.debugLineNum = 185;BA.debugLine="b.Enabled = False";
_b.setEnabled(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 187;BA.debugLine="If wordpick.IndexOf(b.Text) = 0 Then";
if (mostCurrent._wordpick.indexOf(_b.getText())==0) { 
 //BA.debugLineNum = 188;BA.debugLine="Label1.Text = b.Text";
mostCurrent._label1.setText((Object)(_b.getText()));
 };
 //BA.debugLineNum = 190;BA.debugLine="If wordpick.IndexOf(b.Text) = 1 Then";
if (mostCurrent._wordpick.indexOf(_b.getText())==1) { 
 //BA.debugLineNum = 191;BA.debugLine="Label2.Text = b.Text";
mostCurrent._label2.setText((Object)(_b.getText()));
 };
 //BA.debugLineNum = 193;BA.debugLine="If wordpick.IndexOf(b.Text) = 2 Then";
if (mostCurrent._wordpick.indexOf(_b.getText())==2) { 
 //BA.debugLineNum = 194;BA.debugLine="Label3.Text = b.Text";
mostCurrent._label3.setText((Object)(_b.getText()));
 };
 //BA.debugLineNum = 196;BA.debugLine="If wordpick.IndexOf(b.Text) = 3 Then";
if (mostCurrent._wordpick.indexOf(_b.getText())==3) { 
 //BA.debugLineNum = 197;BA.debugLine="Label4.Text = b.Text";
mostCurrent._label4.setText((Object)(_b.getText()));
 };
 //BA.debugLineNum = 199;BA.debugLine="If wordpick.IndexOf(b.Text) = 4 Then";
if (mostCurrent._wordpick.indexOf(_b.getText())==4) { 
 //BA.debugLineNum = 200;BA.debugLine="Label5.Text = b.Text";
mostCurrent._label5.setText((Object)(_b.getText()));
 };
 //BA.debugLineNum = 202;BA.debugLine="If wordpick.IndexOf(b.Text) = 5 Then";
if (mostCurrent._wordpick.indexOf(_b.getText())==5) { 
 //BA.debugLineNum = 203;BA.debugLine="Label6.Text = b.Text";
mostCurrent._label6.setText((Object)(_b.getText()));
 };
 //BA.debugLineNum = 205;BA.debugLine="If wordpick.IndexOf(b.Text) = 6 Then";
if (mostCurrent._wordpick.indexOf(_b.getText())==6) { 
 //BA.debugLineNum = 206;BA.debugLine="Label7.Text = b.Text";
mostCurrent._label7.setText((Object)(_b.getText()));
 };
 //BA.debugLineNum = 208;BA.debugLine="If wordpick.IndexOf(b.Text) = 7 Then";
if (mostCurrent._wordpick.indexOf(_b.getText())==7) { 
 //BA.debugLineNum = 209;BA.debugLine="Label8.Text = b.Text";
mostCurrent._label8.setText((Object)(_b.getText()));
 };
 //BA.debugLineNum = 211;BA.debugLine="If wordpick.IndexOf(b.Text) = 8 Then";
if (mostCurrent._wordpick.indexOf(_b.getText())==8) { 
 //BA.debugLineNum = 212;BA.debugLine="Label9.Text = b.Text";
mostCurrent._label9.setText((Object)(_b.getText()));
 };
 //BA.debugLineNum = 214;BA.debugLine="If wordpick.IndexOf(b.Text) = 9 Then";
if (mostCurrent._wordpick.indexOf(_b.getText())==9) { 
 //BA.debugLineNum = 215;BA.debugLine="Label10.Text = b.Text";
mostCurrent._label10.setText((Object)(_b.getText()));
 };
 //BA.debugLineNum = 217;BA.debugLine="If wordpick.IndexOf(b.Text) = 10 Then";
if (mostCurrent._wordpick.indexOf(_b.getText())==10) { 
 //BA.debugLineNum = 218;BA.debugLine="Label11.Text = b.Text";
mostCurrent._label11.setText((Object)(_b.getText()));
 };
 }else {
 //BA.debugLineNum = 221;BA.debugLine="b.Enabled = False";
_b.setEnabled(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 222;BA.debugLine="counter = counter - 1";
_counter = (int) (_counter-1);
 //BA.debugLineNum = 223;BA.debugLine="counter_lbl.text = \"You have: \" & counter &\" Attempts\"";
mostCurrent._counter_lbl.setText((Object)("You have: "+BA.NumberToString(_counter)+" Attempts"));
 };
 //BA.debugLineNum = 226;BA.debugLine="If counter = 0 Then";
if (_counter==0) { 
 //BA.debugLineNum = 227;BA.debugLine="Msgbox(\"you Lost, the word is : \"& wordpick,\"Game Over\")";
anywheresoftware.b4a.keywords.Common.Msgbox("you Lost, the word is : "+mostCurrent._wordpick,"Game Over",mostCurrent.activityBA);
 //BA.debugLineNum = 228;BA.debugLine="Dim response As Int";
_response = 0;
 //BA.debugLineNum = 229;BA.debugLine="response = Msgbox2(\"You want to play again ?\",\"New Game\",\"Yes\",\"Cancel\",\"No\",LoadBitmap(File.DirAssets,\"bullet.png\"))";
_response = anywheresoftware.b4a.keywords.Common.Msgbox2("You want to play again ?","New Game","Yes","Cancel","No",(android.graphics.Bitmap)(anywheresoftware.b4a.keywords.Common.LoadBitmap(anywheresoftware.b4a.keywords.Common.File.getDirAssets(),"bullet.png").getObject()),mostCurrent.activityBA);
 //BA.debugLineNum = 230;BA.debugLine="If  DialogResponse.POSITIVE = response Then";
if (anywheresoftware.b4a.keywords.Common.DialogResponse.POSITIVE==_response) { 
 //BA.debugLineNum = 231;BA.debugLine="generate = Rnd(0,wordList.Size -1)";
_generate = anywheresoftware.b4a.keywords.Common.Rnd((int) (0),(int) (mostCurrent._wordlist.getSize()-1));
 //BA.debugLineNum = 232;BA.debugLine="For i = 0 To 8";
{
final int step169 = 1;
final int limit169 = (int) (8);
for (_i = (int) (0); (step169 > 0 && _i <= limit169) || (step169 < 0 && _i >= limit169); _i = ((int)(0 + _i + step169))) {
 //BA.debugLineNum = 233;BA.debugLine="For j = 0 To 2";
{
final int step170 = 1;
final int limit170 = (int) (2);
for (_j = (int) (0); (step170 > 0 && _j <= limit170) || (step170 < 0 && _j >= limit170); _j = ((int)(0 + _j + step170))) {
 //BA.debugLineNum = 234;BA.debugLine="buttons(i,j).Enabled = True";
mostCurrent._buttons[_i][_j].setEnabled(anywheresoftware.b4a.keywords.Common.True);
 }
};
 }
};
 //BA.debugLineNum = 237;BA.debugLine="Label1.Text =\"\"";
mostCurrent._label1.setText((Object)(""));
 //BA.debugLineNum = 238;BA.debugLine="Label2.Text =\"\"";
mostCurrent._label2.setText((Object)(""));
 //BA.debugLineNum = 239;BA.debugLine="Label3.Text =\"\"";
mostCurrent._label3.setText((Object)(""));
 //BA.debugLineNum = 240;BA.debugLine="Label4.Text =\"\"";
mostCurrent._label4.setText((Object)(""));
 //BA.debugLineNum = 241;BA.debugLine="Label5.Text =\"\"";
mostCurrent._label5.setText((Object)(""));
 //BA.debugLineNum = 242;BA.debugLine="Label6.Text =\"\"";
mostCurrent._label6.setText((Object)(""));
 //BA.debugLineNum = 243;BA.debugLine="Label7.Text =\"\"";
mostCurrent._label7.setText((Object)(""));
 //BA.debugLineNum = 244;BA.debugLine="Label8.Text =\"\"";
mostCurrent._label8.setText((Object)(""));
 //BA.debugLineNum = 245;BA.debugLine="Label9.Text =\"\"";
mostCurrent._label9.setText((Object)(""));
 //BA.debugLineNum = 246;BA.debugLine="Label10.Text =\"\"";
mostCurrent._label10.setText((Object)(""));
 //BA.debugLineNum = 247;BA.debugLine="Label11.Text =\"\"";
mostCurrent._label11.setText((Object)(""));
 //BA.debugLineNum = 248;BA.debugLine="counter = 6";
_counter = (int) (6);
 //BA.debugLineNum = 249;BA.debugLine="wordpick = wordList.Get(generate)";
mostCurrent._wordpick = BA.ObjectToString(mostCurrent._wordlist.Get(_generate));
 //BA.debugLineNum = 250;BA.debugLine="counter_lbl.text = \"You have: \" & counter &\" Attempts\"";
mostCurrent._counter_lbl.setText((Object)("You have: "+BA.NumberToString(_counter)+" Attempts"));
 //BA.debugLineNum = 251;BA.debugLine="letters_lbl.Text = \"The word has: \" & wordpick.Length &\" Letters\"";
mostCurrent._letters_lbl.setText((Object)("The word has: "+BA.NumberToString(mostCurrent._wordpick.length())+" Letters"));
 }else {
 //BA.debugLineNum = 253;BA.debugLine="Activity.Finish";
mostCurrent._activity.Finish();
 };
 }else {
 //BA.debugLineNum = 256;BA.debugLine="If wordpick = Label1.Text & Label2.Text & Label3.Text & Label4.Text & Label5.Text & Label6.Text & Label7.Text & Label8.Text & Label9.Text & Label10.Text & Label11.Text Then";
if ((mostCurrent._wordpick).equals(mostCurrent._label1.getText()+mostCurrent._label2.getText()+mostCurrent._label3.getText()+mostCurrent._label4.getText()+mostCurrent._label5.getText()+mostCurrent._label6.getText()+mostCurrent._label7.getText()+mostCurrent._label8.getText()+mostCurrent._label9.getText()+mostCurrent._label10.getText()+mostCurrent._label11.getText())) { 
 //BA.debugLineNum = 257;BA.debugLine="Msgbox(\"You Found the Word\" ,\"Game Over\")";
anywheresoftware.b4a.keywords.Common.Msgbox("You Found the Word","Game Over",mostCurrent.activityBA);
 //BA.debugLineNum = 258;BA.debugLine="Dim response As Int";
_response = 0;
 //BA.debugLineNum = 259;BA.debugLine="response = Msgbox2(\"You want to play again ?\",\"New Game\",\"Yes\",\"Cancel\",\"No\",LoadBitmap(File.DirAssets,\"bullet.png\"))";
_response = anywheresoftware.b4a.keywords.Common.Msgbox2("You want to play again ?","New Game","Yes","Cancel","No",(android.graphics.Bitmap)(anywheresoftware.b4a.keywords.Common.LoadBitmap(anywheresoftware.b4a.keywords.Common.File.getDirAssets(),"bullet.png").getObject()),mostCurrent.activityBA);
 //BA.debugLineNum = 260;BA.debugLine="If  DialogResponse.POSITIVE = response Then";
if (anywheresoftware.b4a.keywords.Common.DialogResponse.POSITIVE==_response) { 
 //BA.debugLineNum = 261;BA.debugLine="generate = Rnd(0,wordList.Size -1)";
_generate = anywheresoftware.b4a.keywords.Common.Rnd((int) (0),(int) (mostCurrent._wordlist.getSize()-1));
 //BA.debugLineNum = 262;BA.debugLine="For i = 0 To 8";
{
final int step199 = 1;
final int limit199 = (int) (8);
for (_i = (int) (0); (step199 > 0 && _i <= limit199) || (step199 < 0 && _i >= limit199); _i = ((int)(0 + _i + step199))) {
 //BA.debugLineNum = 263;BA.debugLine="For j = 0 To 2";
{
final int step200 = 1;
final int limit200 = (int) (2);
for (_j = (int) (0); (step200 > 0 && _j <= limit200) || (step200 < 0 && _j >= limit200); _j = ((int)(0 + _j + step200))) {
 //BA.debugLineNum = 264;BA.debugLine="buttons(i,j).Enabled = True";
mostCurrent._buttons[_i][_j].setEnabled(anywheresoftware.b4a.keywords.Common.True);
 }
};
 }
};
 //BA.debugLineNum = 267;BA.debugLine="Label1.Text =\"\"";
mostCurrent._label1.setText((Object)(""));
 //BA.debugLineNum = 268;BA.debugLine="Label2.Text =\"\"";
mostCurrent._label2.setText((Object)(""));
 //BA.debugLineNum = 269;BA.debugLine="Label3.Text =\"\"";
mostCurrent._label3.setText((Object)(""));
 //BA.debugLineNum = 270;BA.debugLine="Label4.Text =\"\"";
mostCurrent._label4.setText((Object)(""));
 //BA.debugLineNum = 271;BA.debugLine="Label5.Text =\"\"";
mostCurrent._label5.setText((Object)(""));
 //BA.debugLineNum = 272;BA.debugLine="Label6.Text =\"\"";
mostCurrent._label6.setText((Object)(""));
 //BA.debugLineNum = 273;BA.debugLine="Label7.Text =\"\"";
mostCurrent._label7.setText((Object)(""));
 //BA.debugLineNum = 274;BA.debugLine="Label8.Text =\"\"";
mostCurrent._label8.setText((Object)(""));
 //BA.debugLineNum = 275;BA.debugLine="Label9.Text =\"\"";
mostCurrent._label9.setText((Object)(""));
 //BA.debugLineNum = 276;BA.debugLine="Label10.Text =\"\"";
mostCurrent._label10.setText((Object)(""));
 //BA.debugLineNum = 277;BA.debugLine="Label11.Text =\"\"";
mostCurrent._label11.setText((Object)(""));
 //BA.debugLineNum = 278;BA.debugLine="counter = 6";
_counter = (int) (6);
 //BA.debugLineNum = 279;BA.debugLine="wordpick = wordList.Get(generate)";
mostCurrent._wordpick = BA.ObjectToString(mostCurrent._wordlist.Get(_generate));
 //BA.debugLineNum = 280;BA.debugLine="counter_lbl.text = \"You have: \" & counter &\" Attempts\"";
mostCurrent._counter_lbl.setText((Object)("You have: "+BA.NumberToString(_counter)+" Attempts"));
 //BA.debugLineNum = 281;BA.debugLine="letters_lbl.Text = \"The word has: \" & wordpick.Length &\" Letters\"";
mostCurrent._letters_lbl.setText((Object)("The word has: "+BA.NumberToString(mostCurrent._wordpick.length())+" Letters"));
 }else {
 //BA.debugLineNum = 283;BA.debugLine="Activity.Finish";
mostCurrent._activity.Finish();
 };
 };
 };
 //BA.debugLineNum = 289;BA.debugLine="End Sub";
return "";
}

public static void initializeProcessGlobals() {
    
    if (main.processGlobalsRun == false) {
	    main.processGlobalsRun = true;
		try {
		        main._process_globals();
		
        } catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
}public static String  _globals() throws Exception{
 //BA.debugLineNum = 21;BA.debugLine="Sub Globals";
 //BA.debugLineNum = 24;BA.debugLine="Dim buttons(9,3) As Button";
mostCurrent._buttons = new anywheresoftware.b4a.objects.ButtonWrapper[(int) (9)][];
{
int d0 = mostCurrent._buttons.length;
int d1 = (int) (3);
for (int i0 = 0;i0 < d0;i0++) {
mostCurrent._buttons[i0] = new anywheresoftware.b4a.objects.ButtonWrapper[d1];
for (int i1 = 0;i1 < d1;i1++) {
mostCurrent._buttons[i0][i1] = new anywheresoftware.b4a.objects.ButtonWrapper();
}
}
}
;
 //BA.debugLineNum = 25;BA.debugLine="Dim wordList As List";
mostCurrent._wordlist = new anywheresoftware.b4a.objects.collections.List();
 //BA.debugLineNum = 26;BA.debugLine="Dim counter As Int = 6";
_counter = (int) (6);
 //BA.debugLineNum = 27;BA.debugLine="Dim character() As String";
mostCurrent._character = new String[(int) (0)];
java.util.Arrays.fill(mostCurrent._character,"");
 //BA.debugLineNum = 28;BA.debugLine="Dim k As Int = 0";
_k = (int) (0);
 //BA.debugLineNum = 29;BA.debugLine="Dim i , j As Int";
_i = 0;
_j = 0;
 //BA.debugLineNum = 30;BA.debugLine="character = Array As String (\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\",\"j\",\"k\",\"l\",\"m\",\"n\",\"o\",\"p\",\"q\",\"r\",\"s\",\"t\",\"u\",\"v\",\"w\",\"x\",\"y\",\"z\",\" \",\" \",\" \")";
mostCurrent._character = new String[]{"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"," "," "," "};
 //BA.debugLineNum = 32;BA.debugLine="Dim generate As Int";
_generate = 0;
 //BA.debugLineNum = 38;BA.debugLine="Private Label1 As Label";
mostCurrent._label1 = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 39;BA.debugLine="Private Label2 As Label";
mostCurrent._label2 = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 40;BA.debugLine="Private Label3 As Label";
mostCurrent._label3 = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 41;BA.debugLine="Private Label4 As Label";
mostCurrent._label4 = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 42;BA.debugLine="Private Label5 As Label";
mostCurrent._label5 = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 43;BA.debugLine="Private Label6 As Label";
mostCurrent._label6 = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 44;BA.debugLine="Private Label10 As Label";
mostCurrent._label10 = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 45;BA.debugLine="Private Label7 As Label";
mostCurrent._label7 = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 46;BA.debugLine="Private Label8 As Label";
mostCurrent._label8 = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 47;BA.debugLine="Private Label9 As Label";
mostCurrent._label9 = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 48;BA.debugLine="Dim wordpick As String";
mostCurrent._wordpick = "";
 //BA.debugLineNum = 49;BA.debugLine="Private letters_lbl As Label";
mostCurrent._letters_lbl = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 50;BA.debugLine="Private counter_lbl As Label";
mostCurrent._counter_lbl = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 51;BA.debugLine="Private guess_lbl As Label";
mostCurrent._guess_lbl = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 52;BA.debugLine="Private Label11 As Label";
mostCurrent._label11 = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 53;BA.debugLine="Private Panel1 As Panel";
mostCurrent._panel1 = new anywheresoftware.b4a.objects.PanelWrapper();
 //BA.debugLineNum = 54;BA.debugLine="Private Panel2 As Panel";
mostCurrent._panel2 = new anywheresoftware.b4a.objects.PanelWrapper();
 //BA.debugLineNum = 55;BA.debugLine="End Sub";
return "";
}
public static String  _process_globals() throws Exception{
 //BA.debugLineNum = 15;BA.debugLine="Sub Process_Globals";
 //BA.debugLineNum = 19;BA.debugLine="End Sub";
return "";
}
public static String  _restart_click() throws Exception{
 //BA.debugLineNum = 144;BA.debugLine="Sub restart_Click";
 //BA.debugLineNum = 145;BA.debugLine="Label1.Text =\"\"";
mostCurrent._label1.setText((Object)(""));
 //BA.debugLineNum = 146;BA.debugLine="Label2.Text =\"\"";
mostCurrent._label2.setText((Object)(""));
 //BA.debugLineNum = 147;BA.debugLine="Label3.Text =\"\"";
mostCurrent._label3.setText((Object)(""));
 //BA.debugLineNum = 148;BA.debugLine="Label4.Text =\"\"";
mostCurrent._label4.setText((Object)(""));
 //BA.debugLineNum = 149;BA.debugLine="Label5.Text =\"\"";
mostCurrent._label5.setText((Object)(""));
 //BA.debugLineNum = 150;BA.debugLine="Label6.Text =\"\"";
mostCurrent._label6.setText((Object)(""));
 //BA.debugLineNum = 151;BA.debugLine="Label7.Text =\"\"";
mostCurrent._label7.setText((Object)(""));
 //BA.debugLineNum = 152;BA.debugLine="Label8.Text =\"\"";
mostCurrent._label8.setText((Object)(""));
 //BA.debugLineNum = 153;BA.debugLine="Label9.Text =\"\"";
mostCurrent._label9.setText((Object)(""));
 //BA.debugLineNum = 154;BA.debugLine="Label10.Text =\"\"";
mostCurrent._label10.setText((Object)(""));
 //BA.debugLineNum = 155;BA.debugLine="Label11.Text =\"\"";
mostCurrent._label11.setText((Object)(""));
 //BA.debugLineNum = 156;BA.debugLine="counter = 6";
_counter = (int) (6);
 //BA.debugLineNum = 157;BA.debugLine="wordpick = wordList.Get(generate)";
mostCurrent._wordpick = BA.ObjectToString(mostCurrent._wordlist.Get(_generate));
 //BA.debugLineNum = 158;BA.debugLine="counter_lbl.text = \"You have: \" & counter &\" Attempts\"";
mostCurrent._counter_lbl.setText((Object)("You have: "+BA.NumberToString(_counter)+" Attempts"));
 //BA.debugLineNum = 159;BA.debugLine="letters_lbl.Text = \"The word has: \" & wordpick.Length &\" Letters\"";
mostCurrent._letters_lbl.setText((Object)("The word has: "+BA.NumberToString(mostCurrent._wordpick.length())+" Letters"));
 //BA.debugLineNum = 160;BA.debugLine="generate = Rnd(0,wordList.Size -1)";
_generate = anywheresoftware.b4a.keywords.Common.Rnd((int) (0),(int) (mostCurrent._wordlist.getSize()-1));
 //BA.debugLineNum = 161;BA.debugLine="For i = 0 To 8";
{
final int step109 = 1;
final int limit109 = (int) (8);
for (_i = (int) (0); (step109 > 0 && _i <= limit109) || (step109 < 0 && _i >= limit109); _i = ((int)(0 + _i + step109))) {
 //BA.debugLineNum = 162;BA.debugLine="For j = 0 To 2";
{
final int step110 = 1;
final int limit110 = (int) (2);
for (_j = (int) (0); (step110 > 0 && _j <= limit110) || (step110 < 0 && _j >= limit110); _j = ((int)(0 + _j + step110))) {
 //BA.debugLineNum = 163;BA.debugLine="buttons(i,j).Enabled = True";
mostCurrent._buttons[_i][_j].setEnabled(anywheresoftware.b4a.keywords.Common.True);
 }
};
 }
};
 //BA.debugLineNum = 166;BA.debugLine="End Sub";
return "";
}
}
