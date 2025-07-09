package com.manuelmaly.hn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import com.manuelmaly.hn.model.HNPost;
import com.manuelmaly.hn.util.FontHelper;

public class GeckoViewActivity extends AppCompatActivity {
    private static GeckoRuntime sRuntime;
    private HNPost mPost;
    private TextView mActionbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.geckoview_activity);

        // Get the post from intent
        mPost = (HNPost) getIntent().getSerializableExtra("post");
        
        // Setup action bar
        setupActionBar();

        GeckoView view = findViewById(R.id.geckoview);
        GeckoSession session = new GeckoSession();

        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this);
        }

        session.open(sRuntime);

        String url = getIntent().getStringExtra("url");
        if (url != null) {
            session.loadUri(url);
        } else if (mPost != null) {
            session.loadUri(mPost.getURL());
        } else {
            session.loadUri("https://news.ycombinator.com/");
        }
        view.setSession(session);
    }

    private void setupActionBar() {
        // Set custom action bar layout
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setCustomView(R.layout.actionbar_center_title);
        
        // Enable the back arrow
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mActionbarTitle = (TextView) getSupportActionBar().getCustomView().findViewById(R.id.actionbar_title);
        mActionbarTitle.setTypeface(FontHelper.getComfortaa(this, true));
        mActionbarTitle.setText(getString(R.string.article));
        
        // Make the title clickable to switch to comments
        mActionbarTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPost != null) {
                    Intent intent = new Intent(GeckoViewActivity.this, CommentsActivity_.class);
                    intent.putExtra(CommentsActivity.EXTRA_HNPOST, mPost);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Handle the back arrow click
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
} 