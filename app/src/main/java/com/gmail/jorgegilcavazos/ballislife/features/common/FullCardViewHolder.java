package com.gmail.jorgegilcavazos.ballislife.features.common;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthentication;
import com.gmail.jorgegilcavazos.ballislife.features.model.SubmissionWrapper;
import com.gmail.jorgegilcavazos.ballislife.util.DateFormatUtil;
import com.gmail.jorgegilcavazos.ballislife.util.Pair;
import com.gmail.jorgegilcavazos.ballislife.util.RedditUtils;
import com.gmail.jorgegilcavazos.ballislife.util.Utilities;
import com.google.common.base.Optional;
import com.squareup.picasso.Picasso;

import net.dean.jraw.models.VoteDirection;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FullCardViewHolder extends RecyclerView.ViewHolder {

    public @BindView(R.id.text_title) TextView tvTitle;
    public @BindView(R.id.text_author) TextView tvAuthor;
    public @BindView(R.id.text_timestamp) TextView tvTimestamp;
    public @BindView(R.id.text_domain) TextView tvDomain;
    public @BindView(R.id.image_thumbnail) ImageView ivThumbnail;
    public @BindView(R.id.button_upvote) ImageButton btnUpvote;
    public @BindView(R.id.text_points) TextView tvPoints;
    public @BindView(R.id.button_downvote) ImageButton btnDownvote;
    public @BindView(R.id.button_save) ImageButton btnSave;
    public @BindView(R.id.text_body) TextView tvBody;
    public @BindView(R.id.content_link) LinearLayout containerLink;
    public @BindView(R.id.text_domain_link) TextView tvDomainLink;
    public @BindView(R.id.text_link) TextView tvLink;
    public @BindView(R.id.header_layout) View  headerLayout;

    public FullCardViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bindData(
            final Context context,
            final RedditAuthentication redditAuthentication, final SubmissionWrapper
                    submissionWrapper,
            boolean isDisplayedInList,
            final OnSubmissionClickListener submissionClickListener) {

        String title, author, commentCount, score, selfTextHtml, domain, thumbnail,
                highResThumbnail, thumbnailToShow;
        final String url;
        boolean isSelf, isStickied, isSaved;
        VoteDirection vote;
        long timestamp;

        // Get data from real submission if available, otherwise used data from fake one.
        title = submissionWrapper.getTitle();
        author = submissionWrapper.getAuthor();
        timestamp = submissionWrapper.getCreated();
        commentCount = String.valueOf(submissionWrapper.getCommentCount());
        score = String.valueOf(submissionWrapper.getScore());
        selfTextHtml = submissionWrapper.getSelfTextHtml();
        domain = submissionWrapper.getDomain();
        url = submissionWrapper.getUrl();
        isSelf = submissionWrapper.isSelfPost();
        isStickied = submissionWrapper.isStickied();
        isSaved = submissionWrapper.isSaved();
        vote = submissionWrapper.getVoteDirection();

        Optional<Pair<Utilities.ThumbnailType, String>> thumbnailTypeUrl = Utilities
                .getThumbnailToShowFromCustomSubmission(submissionWrapper);
        thumbnailToShow = thumbnailTypeUrl.isPresent() ? thumbnailTypeUrl.get().second : null;

        // Bind data to views.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvTitle.setText(Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvTitle.setText(Html.fromHtml(title));
        }

        tvAuthor.setText(author);
        tvTimestamp.setText(DateFormatUtil.formatRedditDate(new Date(timestamp)));
        tvPoints.setText(score);

        tvBody.setOnTouchListener((v, event) -> {
            boolean ret = false;
            CharSequence text = ((TextView) v).getText();
            Spannable stext = Spannable.Factory.getInstance().newSpannable(text);
            TextView widget = (TextView) v;
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                ClickableSpan[] link = stext.getSpans(off, off, ClickableSpan.class);

                if (link.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        link[0].onClick(widget);
                    }
                    ret = true;
                }
            }
            return ret;
        });

        if (isSelf) {
            if (!isDisplayedInList && selfTextHtml != null) {
                tvBody.setVisibility(View.VISIBLE);
                tvBody.setText(RedditUtils.bindSnuDown(selfTextHtml));
            } else {
                tvBody.setVisibility(View.GONE);
            }
            tvDomain.setText("self");
            ivThumbnail.setVisibility(View.GONE);
            containerLink.setVisibility(View.GONE);
        } else {
            tvBody.setVisibility(View.GONE);
            tvDomain.setText(domain);
            if (thumbnailToShow != null) {
                ivThumbnail.setVisibility(View.VISIBLE);
                containerLink.setVisibility(View.GONE);
                Picasso.with(context)
                        .load(thumbnailToShow)
                        .into(ivThumbnail);
            } else {
                ivThumbnail.setVisibility(View.GONE);
                containerLink.setVisibility(View.VISIBLE);
                tvDomainLink.setText(domain);
                tvLink.setText(url);
            }
        }

        // Set title font to green and bold if is stickied post.
        if (isStickied) {
            tvTitle.setTextColor(ContextCompat.getColor(context, R.color.stickiedColor));
            tvTitle.setTypeface(null, Typeface.BOLD);
        } else {
            tvTitle.setTextColor(ContextCompat.getColor(context, R.color.primaryText));
            tvTitle.setTypeface(null, Typeface.NORMAL);
        }

        // Set vote buttons colors if the submission has been voted on.
        if (vote == VoteDirection.UPVOTE) {
            setUpvotedColors(context);
        } else if (vote == VoteDirection.DOWNVOTE) {
            setDownvotedColors(context);
        } else {
            setNoVoteColors(context);
        }

        // Set saved button color depending on whether the submission has been saved.
        if (isSaved) {
            setSavedIcon();
        } else {
            setUnsavedIcon();
        }

        final FullCardViewHolder holder = this;

        btnUpvote.setOnClickListener(v -> {
            if (submissionWrapper.getVoteDirection() == VoteDirection.UPVOTE) {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.NO_VOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.NO_VOTE);
                    setNoVoteColors(context);
                }
            } else if (submissionWrapper.getVoteDirection() == VoteDirection.DOWNVOTE) {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.UPVOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.UPVOTE);
                    setUpvotedColors(context);
                }
            } else {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.UPVOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.UPVOTE);
                    setUpvotedColors(context);
                }
            }
        });

        btnDownvote.setOnClickListener(v -> {
            if (submissionWrapper.getVoteDirection() == VoteDirection.DOWNVOTE) {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.NO_VOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.NO_VOTE);
                    setNoVoteColors(context);
                }
            } else if (submissionWrapper.getVoteDirection() == VoteDirection.UPVOTE) {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.DOWNVOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.DOWNVOTE);
                    setDownvotedColors(context);
                }
            } else {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.DOWNVOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.DOWNVOTE);
                    setDownvotedColors(context);
                }
            }
        });

        btnSave.setOnClickListener(v -> {
            if (submissionWrapper.isSaved()) {
                submissionClickListener.onSaveSubmission(submissionWrapper, false);
                if (redditAuthentication.isUserLoggedIn()) {
                    setUnsavedIcon();
                    submissionWrapper.setSaved(false);
                }
            } else {
                submissionClickListener.onSaveSubmission(submissionWrapper, true);
                if (redditAuthentication.isUserLoggedIn()) {
                    setSavedIcon();
                    submissionWrapper.setSaved(true);
                }
            }
        });

        ivThumbnail.setOnClickListener(v -> submissionClickListener.onContentClick(url));

        containerLink.setOnClickListener(v -> submissionClickListener.onContentClick(url));

        if (isDisplayedInList) {
            // Enable buttons to navigate to SubmissionActivity.
            headerLayout.setOnClickListener(v -> submissionClickListener.onSubmissionClick
                    (submissionWrapper));
        }
    }

    private void setUpvotedColors(Context context) {
        setUpvoteIcon(true);
        setDownvoteIcon(false);
        tvPoints.setTextColor(ContextCompat.getColor(context, R.color.commentUpvoted));
    }

    private void setDownvotedColors(Context context) {
        setUpvoteIcon(false);
        setDownvoteIcon(true);
        tvPoints.setTextColor(ContextCompat.getColor(context, R.color.commentDownvoted));
    }

    private void setNoVoteColors(Context context) {
        setUpvoteIcon(false);
        setDownvoteIcon(false);
        tvPoints.setTextColor(ContextCompat.getColor(context, R.color.commentNeutral));
    }

    private void setUpvoteIcon(boolean active) {
        if (active) {
            btnUpvote.setImageResource(R.drawable.ic_arrow_upward_orange_18dp);
        } else {
            btnUpvote.setImageResource(R.drawable.ic_arrow_upward_black_18dp);
        }
    }

    private void setDownvoteIcon(boolean active) {
        if (active) {
            btnDownvote.setImageResource(R.drawable.ic_arrow_downward_purple_18dp);
        } else {
            btnDownvote.setImageResource(R.drawable.ic_arrow_downward_black_18dp);
        }
    }

    private void setSavedIcon() {
        btnSave.setImageResource(R.drawable.ic_bookmark_black_18dp);
    }

    private void setUnsavedIcon() {
        btnSave.setImageResource(R.drawable.ic_bookmark_border_black_18dp);
    }
}
