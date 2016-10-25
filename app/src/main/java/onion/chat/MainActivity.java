

package onion.chat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Database db;
    Tor tor;
    TabLayout tabLayout;
    View contactPage, requestPage;
    RecyclerView contactRecycler, requestRecycler;
    View contactEmpty, requestEmpty;
    Cursor contactCursor, requestCursor;

    TextToSpeech t1;
    int REQUEST_QR = 12;

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1001;
    void send() {
        Client.getInstance(this).startSendPendingFriends();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = Database.getInstance(this);
        tor = Tor.getInstance(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                final Dialog[] d = new Dialog[1];

                View v = getLayoutInflater().inflate(R.layout.dialog_connect, null);
                ((TextView) v.findViewById(R.id.id)).setText(Tor.getInstance(MainActivity.this).getID());
                v.findViewById(R.id.qr_show).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d[0].cancel();
                        showQR();
                    }
                });
                v.findViewById(R.id.qr_scan).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d[0].cancel();
                        scanQR();
                    }
                });
                v.findViewById(R.id.enter_id).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d[0].cancel();
                        addContact();
                    }
                });
                v.findViewById(R.id.share_id).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d[0].cancel();
                        inviteFriend();
                    }
                });
                d[0] = new AlertDialog.Builder(MainActivity.this)
                        //.setTitle(R.string.add_contact)
                        .setView(v)
                        .show();

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        startService(new Intent(this, HostService.class));



        findViewById(R.id.myname).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeName();
            }
        });


        findViewById(R.id.myaddress).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQR();
            }
        });


        contactPage = getLayoutInflater().inflate(R.layout.page_contacts, null);
        requestPage = getLayoutInflater().inflate(R.layout.page_requests, null);

        contactRecycler = (RecyclerView) contactPage.findViewById(R.id.contactRecycler);
        requestRecycler = (RecyclerView) requestPage.findViewById(R.id.requestRecycler);

        contactEmpty = contactPage.findViewById(R.id.contactEmpty);
        requestEmpty = requestPage.findViewById(R.id.requestEmpty);

        contactRecycler.setLayoutManager(new LinearLayoutManager(this));
        contactRecycler.setAdapter(new RecyclerView.Adapter<ContactViewHolder>() {
            @Override
            public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                final ContactViewHolder viewHolder = new ContactViewHolder(getLayoutInflater().inflate(R.layout.item_contact, parent, false));
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String toSpeak=viewHolder.name.getText().toString();
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("chat:" + viewHolder.address.getText()), getApplicationContext(), ChatActivity.class));
                    }
                });
                viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        contactLongPress(viewHolder.address.getText().toString(), viewHolder.name.getText().toString());
                        return true;
                    }
                });
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(ContactViewHolder holder, int position) {
                contactCursor.moveToPosition(position);
                holder.address.setText(contactCursor.getString(0));
                String name = contactCursor.getString(1);
                if (name == null || name.equals("")) name = "Anonymous";
                holder.name.setText(name);
                long n = contactCursor.getLong(2);
                if (n > 0) {
                    holder.badge.setVisibility(View.VISIBLE);
                    holder.count.setText("" + n);
                } else {
                    holder.badge.setVisibility(View.GONE);
                }
            }

            @Override
            public int getItemCount() {
                return contactCursor != null ? contactCursor.getCount() : 0;
            }
        });

        requestRecycler.setLayoutManager(new LinearLayoutManager(this));
        requestRecycler.setAdapter(new RecyclerView.Adapter<ContactViewHolder>() {
            @Override
            public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                final ContactViewHolder viewHolder = new ContactViewHolder(getLayoutInflater().inflate(R.layout.item_contact_request, parent, false));
                viewHolder.accept.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String addr = viewHolder.address.getText().toString();
                        db.acceptContact(addr);
                        Client.getInstance(getApplicationContext()).startAskForNewMessages(addr);
                        updateContactList();
                    }
                });
                viewHolder.decline.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String address = viewHolder.address.getText().toString();
                        final String name = viewHolder.name.getText().toString();
                        db.removeContact(address);
                        updateContactList();
                        Snackbar.make(findViewById(R.id.drawer_layout), R.string.contact_request_declined, Snackbar.LENGTH_LONG)
                                .setAction(R.string.undo, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        db.addContact(address, false, true, name);
                                        updateContactList();
                                    }
                                })
                                .show();
                    }
                });
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(ContactViewHolder holder, int position) {
                requestCursor.moveToPosition(position);
                holder.address.setText(requestCursor.getString(0));
                String name = requestCursor.getString(1);
                if (name == null || name.equals("")) name = "Anonymous";
                holder.name.setText(name);
            }

            @Override
            public int getItemCount() {
                return requestCursor != null ? requestCursor.getCount() : 0;
            }
        });

        tabLayout = (TabLayout) findViewById(R.id.tabLayout);


        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(final ViewGroup container, int position) {
                View v = position == 0 ? contactPage : requestPage;
                container.addView(v);
                return v;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        });
        tabLayout.setupWithViewPager(viewPager);


        tabLayout.getTabAt(0).setText(R.string.tab_contacts);
        tabLayout.getTabAt(1).setText(R.string.tab_requests);


        for (int i = 0; i < 2; i++) {
            View v = getLayoutInflater().inflate(R.layout.tab_header, null, false);
            ((TextView) v.findViewById(R.id.text)).setText(tabLayout.getTabAt(i).getText().toString());
            ((TextView) v.findViewById(R.id.badge)).setText("");
            v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tabLayout.getTabAt(i).setCustomView(v);
        }

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    db.clearNewRequests();
                }
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        updateContactList();

        handleIntent();
      //  inform();
        listen();
    }

    private void inform(){
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });
        String toSpeak="Welcome to Its Ur's an Instant Messaging app!";
        Toast.makeText(getApplicationContext(),
                toSpeak, Toast.LENGTH_SHORT).show();
        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }
    private void listen() {
        //inform();
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            Toast.makeText(this, "Voice recognizer not present",
                    Toast.LENGTH_SHORT).show();
        } else {
            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);


                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass()
                        .getPackage().getName());



                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Its Ur's");

                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
            } catch (Exception e) {
                String toSpeak = "Oops your device doesn't support Voice recognition";
                Toast.makeText(getApplicationContext(),
                        toSpeak, Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }
    void handleIntent() {

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        Uri uri = intent.getData();
        if (uri == null) {
            return;
        }

        if (!uri.getHost().equals("chat.onion")) {
            return;
        }

        List<String> pp = uri.getPathSegments();
        String address = pp.size() > 0 ? pp.get(0) : null;
        String name = pp.size() > 1 ? pp.get(1) : "";
        if (address == null) {
            return;
        }

        addContact(address, name);
        inform();

    }

    void updateContactList() {

        if (contactCursor != null) {
            contactCursor.close();
            contactCursor = null;
        }
        contactCursor = db.getReadableDatabase().query("contacts", new String[]{"address", "name", "pending"}, "incoming=0", null, null, null, "incoming, name, address");
        contactRecycler.getAdapter().notifyDataSetChanged();
        contactEmpty.setVisibility(contactCursor.getCount() == 0 ? View.VISIBLE : View.INVISIBLE);

        if (requestCursor != null) {
            requestCursor.close();
            requestCursor = null;
        }
        requestCursor = db.getReadableDatabase().query("contacts", new String[]{"address", "name"}, "incoming!=0", null, null, null, "incoming, name, address");
        requestRecycler.getAdapter().notifyDataSetChanged();
        requestEmpty.setVisibility(requestCursor.getCount() == 0 ? View.VISIBLE : View.INVISIBLE);


        //updateBadge();

        int newRequests = requestCursor.getCount();
        ((TextView) tabLayout.getTabAt(1).getCustomView().findViewById(R.id.badge)).setText(newRequests > 0 ? "" + newRequests : "");
       // listen();
    }

    /*void updateBadge() {
        int newRequests = db.getNewRequests();
        ((TextView)tabLayout.getTabAt(1).getCustomView().findViewById(R.id.badge)).setText(newRequests > 0 ? "" + newRequests : "");
    }*/

    void contactLongPress(final String address, final String name) {
        View v = getLayoutInflater().inflate(R.layout.dialog_contact, null);
        ((TextView) v.findViewById(R.id.name)).setText(name);
        ((TextView) v.findViewById(R.id.address)).setText(address);
        final Dialog dlg = new AlertDialog.Builder(MainActivity.this)
                .setView(v)
                .create();

        v.findViewById(R.id.openchat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.hide();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("chat:" + address), getApplicationContext(), ChatActivity.class));
            }
        });
        v.findViewById(R.id.changename).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.hide();
                changeContactName(address, name);
            }
        });
        v.findViewById(R.id.copyid).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.hide();
                ((android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setText(address);
                snack(getString(R.string.id_copied_to_clipboard) + address);
            }
        });
        v.findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.hide();
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.delete_contact_q)
                        .setMessage(String.format(getString(R.string.really_delete_contact), address))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                db.removeContact(address);
                                updateContactList();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                //db.removeContact(address);
                //updateContactList();
            }
        });

        dlg.show();
       // inform();
    }




    void changeContactName(final String address, final String name) {
        final FrameLayout view = new FrameLayout(this);
        final EditText editText = new EditText(this);
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        editText.setSingleLine();
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        view.addView(editText);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        ;
        view.setPadding(padding, padding, padding, padding);
        editText.setText(name);
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_change_alias)
                .setView(view)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.setContactName(address,  editText.getText().toString());
                        update();
                        snack(getString(R.string.snack_alias_changed));
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    void update() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.myaddress)).setText(tor.getID());
                ((TextView) findViewById(R.id.myname)).setText(db.getName().trim().isEmpty() ? "Anonymous" : db.getName());
                updateContactList();
                //updateBadge();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Tor.getInstance(this).setListener(new Tor.Listener() {
            @Override
            public void onChange() {
                update();
                send();
            }
        });
        Server.getInstance(this).setListener(new Server.Listener() {
            @Override
            public void onChange() {
                update();
            }
        });
        update();
        send();

        Notifier.getInstance(this).onResumeActivity();

        ((TorStatusView) findViewById(R.id.torStatusView)).update();

        startService(new Intent(this, HostService.class));
    }

    @Override
    protected void onPause() {
        Notifier.getInstance(this).onPauseActivity();
        Tor.getInstance(this).setListener(null);
        Server.getInstance(this).setListener(null);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.changealias) {
            changeName();
            return true;
        }

        if (id == R.id.qr_show) {
            showQR();
            return true;
        }

        if (id == R.id.qr_scan) {
            scanQR();
        }

        if (id == R.id.share_id) {
            inviteFriend();
        }
        if(id==R.id.password){
            changePassword();
        }
        if (id == R.id.copy_id) {
            ((android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setText(tor.getID());
            snack(getString(R.string.id_copied_to_clipboard) + tor.getID());
            return true;
        }


        if (id == R.id.about) {
            showAbout();
        }

        if (id == R.id.enter_id) {
            addContact();
        }

        if (id == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void inviteFriend() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);

        String url = "";

        intent.putExtra(Intent.EXTRA_REFERRER, url);
        intent.putExtra("customAppUri", url);

        String msg = String.format(getString(R.string.invitation_text), url, tor.getID(), Uri.encode(db.getName()));

        Log.i("Message", msg.replace('\n', ' '));

        intent.putExtra(Intent.EXTRA_TEXT, msg);
        intent.setType("text/plain");

        startActivity(intent);
    }

    void scanQR() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_QR);
    }

    void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("Its Urs")
                .setMessage("By Ragavi, Vishali and Sharon")
                .setNeutralButton(R.string.libraries, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showLibraries();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    void showLibraries() {
        final String[] items;
        try {
            items = getResources().getAssets().list("licenses");
        } catch (IOException ex) {
            throw new Error(ex);
        }
        new AlertDialog.Builder(this)
                .setTitle("Third party software used in this app (click to view license)")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showLicense(items[which]);
                    }
                })
                .show();
    }

    void showLicense(String name) {
        String text;
        try {
            text = Utils.str(getResources().getAssets().open("licenses/" + name));
        } catch (IOException ex) {
            throw new Error(ex);
        }
        new AlertDialog.Builder(this)
                .setTitle(name)
                .setMessage(text)
                .show();
    }

    void showQR() {
        String name = db.getName();
        String txt = "Its Ur's " + tor.getID() + " " + name;

        QRCode qr;

        try {
            //qr = Encoder.encode(txt, ErrorCorrectionLevel.H);
            qr = Encoder.encode(txt, ErrorCorrectionLevel.M);
        } catch (Exception ex) {
            throw new Error(ex);
        }

        ByteMatrix mat = qr.getMatrix();
        int width = mat.getWidth();
        int height = mat.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = mat.get(x, y) != 0 ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 8, bitmap.getHeight() * 8, false);

        ImageView view = new ImageView(this);
        view.setImageBitmap(bitmap);

        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        view.setPadding(pad, pad, pad, pad);

        Rect displayRectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        int s = (int) (Math.min(displayRectangle.width(), displayRectangle.height()) * 0.9);
        view.setMinimumWidth(s);
        view.setMinimumHeight(s);
        new AlertDialog.Builder(this)
                //.setMessage(txt)
                .setView(view)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;
        if (requestCode == REQUEST_QR) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            int width = bitmap.getWidth(), height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            bitmap.recycle();
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();
            try {
                Result result = reader.decode(bBitmap);
                String str = result.getText();
                Log.i("ID", str);

                String[] tokens = str.split(" ", 3);

                if (tokens.length < 2 || !tokens[0].equals("Its Ur's")) {
                    snack(getString(R.string.qr_invalid));
                    return;
                }

                String id = tokens[1].toLowerCase();

                if (id.length() != 16) {
                    snack(getString(R.string.qr_invalid));
                    return;
                }

                if (db.hasContact(id)) {
                    snack(getString(R.string.contact_already_added));
                    return;
                }

                String name = "";
                if (tokens.length > 2) {
                    name = tokens[2];
                }

                addContact(id, name);

                return;

            } catch (Exception ex) {
                snack(getString(R.string.qr_invalid));
                ex.printStackTrace();
            }
        }
        else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
                       if (resultCode == RESULT_OK) {
                ArrayList<String> textMatchList = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String[] textTyped= new String[textMatchList.size()];
                String typeText="";
                for(int i=0;i<textMatchList.size();i++){
                    textTyped[i]= textMatchList.get(i);
                    typeText += textTyped[i];
                }
                if (!textMatchList.isEmpty()) {

                    if (textMatchList.get(0).contains("open") || textMatchList.get(0).contains("OPEN")) {
                        String contName="";
                            if(typeText.contains("open chat")) {
                                if (textMatchList.size() >= 2) {
                                    contName = textMatchList.get(2);
                                }
                                if(contName != ""){
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("chat:" + contName), getApplicationContext(), ChatActivity.class));
                                }
                            }
                        listen();
                    }
                    else if (textMatchList.get(0).contains("password") || textMatchList.get(0).contains("PASSWORD")) {
                        String password="password";

                        if(textMatchList.size()>=2) {

                                password = textMatchList.get(0).replaceFirst("password ", "");

                        }
                       else if(textMatchList.size()>=1) {

                                password = textMatchList.get(0).replaceFirst("password ", "");

                        }
                        db.setPassword(password);
                        update();
                        String toSpeak="password changed successfully to "+password;
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                        listen();

                    }
                    else if(textMatchList.get(0).contains("change") || textMatchList.get(0).contains("CHANGE")){
                       String name="";
                        if(textMatchList.size()>=2){
                            name=textMatchList.get(2);
                        }
                        db.setName(name);
                        update();
                        snack(getString(R.string.snack_alias_changed));
                        String toSpeak = "Alias changed to "+name;
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                        listen();
                    }
                    else if(textMatchList.get(0).contains("tell") || textMatchList.get(0).contains("tell")){
                        String id1="";
                        id1 = (tor.getID());
                        Toast.makeText(getApplicationContext(),
                                id1, Toast.LENGTH_SHORT).show();
                        //t1.speak(id1, TextToSpeech.QUEUE_FLUSH, null);
                        listen();
                    }
                    else if(textMatchList.get(0).contains("enter") || textMatchList.get(0).contains("ENTER")){
                        String id1="Yet to come";
                        Toast.makeText(getApplicationContext(),
                                id1, Toast.LENGTH_SHORT).show();
                        //t1.speak(id1, TextToSpeech.QUEUE_FLUSH, null);
                        listen();
                    }
                    else if(textMatchList.get(0).contains("HELP") || textMatchList.get(0).contains("help")){
                        String toSpeak = "Voice Commands that can be used are 1 Open chat with contact name 2 change with alias name 3 tell";
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                        listen();
                    }

                    else if(textMatchList.get(0).contains("close") || textMatchList.get(0).contains("CLOSE")){
                        String toSpeak = "Closing Voice command";
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                    }

                    else {
                        // poString
                        String toSpeak = "I Can't Understand";
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                       // t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                       listen();
                    }

                }
                //Result code for various error.
            } else if (resultCode == RecognizerIntent.RESULT_AUDIO_ERROR) {

                String toSpeak = "Audio Error";
                Toast.makeText(getApplicationContext(),
                        toSpeak, Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            } else if (resultCode == RecognizerIntent.RESULT_CLIENT_ERROR) {

                String toSpeak = "Client Error";
                Toast.makeText(getApplicationContext(),
                        toSpeak, Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            } else if (resultCode == RecognizerIntent.RESULT_NETWORK_ERROR) {

                String toSpeak = "Network Error";
                Toast.makeText(getApplicationContext(),
                        toSpeak, Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            } else if (resultCode == RecognizerIntent.RESULT_NO_MATCH) {

                String toSpeak = "No Match";
                Toast.makeText(getApplicationContext(),
                        toSpeak, Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            } else if (resultCode == RecognizerIntent.RESULT_SERVER_ERROR) {

                String toSpeak = "Server Error";
                Toast.makeText(getApplicationContext(),
                        toSpeak, Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }

        }

    }

    void snack(String s) {
               Snackbar.make(findViewById(R.id.drawer_layout), s, Snackbar.LENGTH_SHORT).show();
    }
    void changePassword(){
        final FrameLayout view = new FrameLayout(this);
        final EditText editText = new EditText(this);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        editText.setSingleLine();
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        view.addView(editText);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        ;
        view.setPadding(padding, padding, padding, padding);
        editText.setText("");
        new AlertDialog.Builder(this)
                .setTitle(R.string.password)
                .setView(view)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.setPassword(editText.getText().toString().trim());
                        update();
                        //snack(getString(R.string.snack_alias_changed));
                        String toSpeak="password changed successfully";
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();

                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }
    void changeName() {
        final FrameLayout view = new FrameLayout(this);
        final EditText editText = new EditText(this);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        editText.setSingleLine();
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        view.addView(editText);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        ;
        view.setPadding(padding, padding, padding, padding);
        editText.setText(db.getName());
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_change_alias)
                .setView(view)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.setName(editText.getText().toString().trim());
                        update();
                        snack(getString(R.string.snack_alias_changed));
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    void addContact() {
        addContact("", "");
    }

    void addContact(String id, String alias) {

        final View view = getLayoutInflater().inflate(R.layout.dialog_add, null);
        final EditText idEd = (EditText) view.findViewById(R.id.add_id);
        idEd.setText(id);
        final EditText aliasEd = (EditText) view.findViewById(R.id.add_alias);
        aliasEd.setText(alias);
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_add_contact)
                .setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String id = idEd.getText().toString().trim();
                        if (id.length() != 16) {
                            snack(getString(R.string.invalid_id));
                            return;
                        }
                        if (id.equals(tor.getID())) {
                            snack(getString(R.string.cant_add_self));
                            return;
                        }
                        if (!db.addContact(id, true, false, aliasEd.getText().toString().trim())) {
                            snack(getString(R.string.failed_to_add_contact));
                            return;
                        }
                        snack(getString(R.string.contact_added));
                        updateContactList();
                        send();
                        tabLayout.getTabAt(0).select();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();

    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView address, name;
        View accept, decline;
        View badge;
        TextView count;

        public ContactViewHolder(View view) {
            super(view);
            address = (TextView) view.findViewById(R.id.address);
            name = (TextView) view.findViewById(R.id.name);
            accept = view.findViewById(R.id.accept);
            decline = view.findViewById(R.id.decline);
            badge = view.findViewById(R.id.badge);
            if (badge != null) count = (TextView) view.findViewById(R.id.count);
        }
    }

}
