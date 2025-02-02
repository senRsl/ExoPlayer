/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2;

import android.content.Context;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioListener;
import com.google.android.exoplayer2.audio.AuxEffectInfo;
import com.google.android.exoplayer2.device.DeviceInfo;
import com.google.android.exoplayer2.device.DeviceListener;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.util.MutableFlags;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoFrameMetadataListener;
import com.google.android.exoplayer2.video.VideoListener;
import com.google.android.exoplayer2.video.spherical.CameraMotionListener;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * A media player interface defining traditional high-level functionality, such as the ability to
 * play, pause, seek and query properties of the currently playing media.
 *
 * <p>Some important properties of media players that implement this interface are:
 *
 * <ul>
 *   <li>They can provide a {@link Timeline} representing the structure of the media being played,
 *       which can be obtained by calling {@link #getCurrentTimeline()}.
 *   <li>They can provide a {@link TrackGroupArray} defining the currently available tracks, which
 *       can be obtained by calling {@link #getCurrentTrackGroups()}.
 *   <li>They contain a number of renderers, each of which is able to render tracks of a single type
 *       (e.g. audio, video or text). The number of renderers and their respective track types can
 *       be obtained by calling {@link #getRendererCount()} and {@link #getRendererType(int)}.
 *   <li>They can provide a {@link TrackSelectionArray} defining which of the currently available
 *       tracks are selected to be rendered by each renderer. This can be obtained by calling {@link
 *       #getCurrentTrackSelections()}}.
 * </ul>
 */
public interface Player {

  /** The audio component of a {@link Player}. */
  interface AudioComponent {

    /**
     * Adds a listener to receive audio events.
     *
     * @param listener The listener to register.
     */
    void addAudioListener(AudioListener listener);

    /**
     * Removes a listener of audio events.
     *
     * @param listener The listener to unregister.
     */
    void removeAudioListener(AudioListener listener);

    /**
     * Sets the attributes for audio playback, used by the underlying audio track. If not set, the
     * default audio attributes will be used. They are suitable for general media playback.
     *
     * <p>Setting the audio attributes during playback may introduce a short gap in audio output as
     * the audio track is recreated. A new audio session id will also be generated.
     *
     * <p>If tunneling is enabled by the track selector, the specified audio attributes will be
     * ignored, but they will take effect if audio is later played without tunneling.
     *
     * <p>If the device is running a build before platform API version 21, audio attributes cannot
     * be set directly on the underlying audio track. In this case, the usage will be mapped onto an
     * equivalent stream type using {@link Util#getStreamTypeForAudioUsage(int)}.
     *
     * <p>If audio focus should be handled, the {@link AudioAttributes#usage} must be {@link
     * C#USAGE_MEDIA} or {@link C#USAGE_GAME}. Other usages will throw an {@link
     * IllegalArgumentException}.
     *
     * @param audioAttributes The attributes to use for audio playback.
     * @param handleAudioFocus True if the player should handle audio focus, false otherwise.
     */
    void setAudioAttributes(AudioAttributes audioAttributes, boolean handleAudioFocus);

    /** Returns the attributes for audio playback. */
    AudioAttributes getAudioAttributes();

    /**
     * Sets the ID of the audio session to attach to the underlying {@link
     * android.media.AudioTrack}.
     *
     * <p>The audio session ID can be generated using {@link C#generateAudioSessionIdV21(Context)}
     * for API 21+.
     *
     * @param audioSessionId The audio session ID, or {@link C#AUDIO_SESSION_ID_UNSET} if it should
     *     be generated by the framework.
     */
    void setAudioSessionId(int audioSessionId);

    /** Returns the audio session identifier, or {@link C#AUDIO_SESSION_ID_UNSET} if not set. */
    int getAudioSessionId();

    /** Sets information on an auxiliary audio effect to attach to the underlying audio track. */
    void setAuxEffectInfo(AuxEffectInfo auxEffectInfo);

    /** Detaches any previously attached auxiliary audio effect from the underlying audio track. */
    void clearAuxEffectInfo();

    /**
     * Sets the audio volume, with 0 being silence and 1 being unity gain.
     *
     * @param audioVolume The audio volume.
     */
    void setVolume(float audioVolume);

    /** Returns the audio volume, with 0 being silence and 1 being unity gain. */
    float getVolume();

    /**
     * Sets whether skipping silences in the audio stream is enabled.
     *
     * @param skipSilenceEnabled Whether skipping silences in the audio stream is enabled.
     */
    void setSkipSilenceEnabled(boolean skipSilenceEnabled);

    /** Returns whether skipping silences in the audio stream is enabled. */
    boolean getSkipSilenceEnabled();
  }

  /** The video component of a {@link Player}. */
  interface VideoComponent {

    /**
     * Sets the {@link C.VideoScalingMode}.
     *
     * @param videoScalingMode The {@link C.VideoScalingMode}.
     */
    void setVideoScalingMode(@C.VideoScalingMode int videoScalingMode);

    /** Returns the {@link C.VideoScalingMode}. */
    @C.VideoScalingMode
    int getVideoScalingMode();

    /**
     * Adds a listener to receive video events.
     *
     * @param listener The listener to register.
     */
    void addVideoListener(VideoListener listener);

    /**
     * Removes a listener of video events.
     *
     * @param listener The listener to unregister.
     */
    void removeVideoListener(VideoListener listener);

    /**
     * Sets a listener to receive video frame metadata events.
     *
     * <p>This method is intended to be called by the same component that sets the {@link Surface}
     * onto which video will be rendered. If using ExoPlayer's standard UI components, this method
     * should not be called directly from application code.
     *
     * @param listener The listener.
     */
    void setVideoFrameMetadataListener(VideoFrameMetadataListener listener);

    /**
     * Clears the listener which receives video frame metadata events if it matches the one passed.
     * Else does nothing.
     *
     * @param listener The listener to clear.
     */
    void clearVideoFrameMetadataListener(VideoFrameMetadataListener listener);

    /**
     * Sets a listener of camera motion events.
     *
     * @param listener The listener.
     */
    void setCameraMotionListener(CameraMotionListener listener);

    /**
     * Clears the listener which receives camera motion events if it matches the one passed. Else
     * does nothing.
     *
     * @param listener The listener to clear.
     */
    void clearCameraMotionListener(CameraMotionListener listener);

    /**
     * Clears any {@link Surface}, {@link SurfaceHolder}, {@link SurfaceView} or {@link TextureView}
     * currently set on the player.
     */
    void clearVideoSurface();

    /**
     * Clears the {@link Surface} onto which video is being rendered if it matches the one passed.
     * Else does nothing.
     *
     * @param surface The surface to clear.
     */
    void clearVideoSurface(@Nullable Surface surface);

    /**
     * Sets the {@link Surface} onto which video will be rendered. The caller is responsible for
     * tracking the lifecycle of the surface, and must clear the surface by calling {@code
     * setVideoSurface(null)} if the surface is destroyed.
     *
     * <p>If the surface is held by a {@link SurfaceView}, {@link TextureView} or {@link
     * SurfaceHolder} then it's recommended to use {@link #setVideoSurfaceView(SurfaceView)}, {@link
     * #setVideoTextureView(TextureView)} or {@link #setVideoSurfaceHolder(SurfaceHolder)} rather
     * than this method, since passing the holder allows the player to track the lifecycle of the
     * surface automatically.
     *
     * @param surface The {@link Surface}.
     */
    void setVideoSurface(@Nullable Surface surface);

    /**
     * Sets the {@link SurfaceHolder} that holds the {@link Surface} onto which video will be
     * rendered. The player will track the lifecycle of the surface automatically.
     *
     * @param surfaceHolder The surface holder.
     */
    void setVideoSurfaceHolder(@Nullable SurfaceHolder surfaceHolder);

    /**
     * Clears the {@link SurfaceHolder} that holds the {@link Surface} onto which video is being
     * rendered if it matches the one passed. Else does nothing.
     *
     * @param surfaceHolder The surface holder to clear.
     */
    void clearVideoSurfaceHolder(@Nullable SurfaceHolder surfaceHolder);

    /**
     * Sets the {@link SurfaceView} onto which video will be rendered. The player will track the
     * lifecycle of the surface automatically.
     *
     * @param surfaceView The surface view.
     */
    void setVideoSurfaceView(@Nullable SurfaceView surfaceView);

    /**
     * Clears the {@link SurfaceView} onto which video is being rendered if it matches the one
     * passed. Else does nothing.
     *
     * @param surfaceView The texture view to clear.
     */
    void clearVideoSurfaceView(@Nullable SurfaceView surfaceView);

    /**
     * Sets the {@link TextureView} onto which video will be rendered. The player will track the
     * lifecycle of the surface automatically.
     *
     * @param textureView The texture view.
     */
    void setVideoTextureView(@Nullable TextureView textureView);

    /**
     * Clears the {@link TextureView} onto which video is being rendered if it matches the one
     * passed. Else does nothing.
     *
     * @param textureView The texture view to clear.
     */
    void clearVideoTextureView(@Nullable TextureView textureView);
  }

  /** The text component of a {@link Player}. */
  interface TextComponent {

    /**
     * Registers an output to receive text events.
     *
     * @param listener The output to register.
     */
    void addTextOutput(TextOutput listener);

    /**
     * Removes a text output.
     *
     * @param listener The output to remove.
     */
    void removeTextOutput(TextOutput listener);

    /** Returns the current {@link Cue Cues}. This list may be empty. */
    List<Cue> getCurrentCues();
  }

  /** The metadata component of a {@link Player}. */
  interface MetadataComponent {

    /**
     * Adds a {@link MetadataOutput} to receive metadata.
     *
     * @param output The output to register.
     */
    void addMetadataOutput(MetadataOutput output);

    /**
     * Removes a {@link MetadataOutput}.
     *
     * @param output The output to remove.
     */
    void removeMetadataOutput(MetadataOutput output);
  }

  /** The device component of a {@link Player}. */
  interface DeviceComponent {

    /** Adds a listener to receive device events. */
    void addDeviceListener(DeviceListener listener);

    /** Removes a listener of device events. */
    void removeDeviceListener(DeviceListener listener);

    /** Gets the device information. */
    DeviceInfo getDeviceInfo();

    /**
     * Gets the current volume of the device.
     *
     * <p>For devices with {@link DeviceInfo#PLAYBACK_TYPE_LOCAL local playback}, the volume
     * returned by this method varies according to the current {@link C.StreamType stream type}. The
     * stream type is determined by {@link AudioAttributes#usage} which can be converted to stream
     * type with {@link Util#getStreamTypeForAudioUsage(int)}. The audio attributes can be set to
     * the player by calling {@link AudioComponent#setAudioAttributes}.
     *
     * <p>For devices with {@link DeviceInfo#PLAYBACK_TYPE_REMOTE remote playback}, the volume of
     * the remote device is returned.
     */
    int getDeviceVolume();

    /** Gets whether the device is muted or not. */
    boolean isDeviceMuted();

    /**
     * Sets the volume of the device.
     *
     * @param volume The volume to set.
     */
    void setDeviceVolume(int volume);

    /** Increases the volume of the device. */
    void increaseDeviceVolume();

    /** Decreases the volume of the device. */
    void decreaseDeviceVolume();

    /** Sets the mute state of the device. */
    void setDeviceMuted(boolean muted);
  }

  /**
   * Listener of changes in player state.
   *
   * <p>All methods have no-op default implementations to allow selective overrides.
   *
   * <p>Listeners can choose to implement individual events (e.g. {@link
   * #onIsPlayingChanged(boolean)}) or {@link #onEvents(Player, Events)}, which is called after one
   * or more events occurred together.
   */
  interface EventListener {

    /**
     * Called when the timeline has been refreshed.
     *
     * <p>Note that if the timeline has changed then a position discontinuity may also have
     * occurred. For example, the current period index may have changed as a result of periods being
     * added or removed from the timeline. This will <em>not</em> be reported via a separate call to
     * {@link #onPositionDiscontinuity(int)}.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param timeline The latest timeline. Never null, but may be empty.
     * @param reason The {@link TimelineChangeReason} responsible for this timeline change.
     */
    @SuppressWarnings("deprecation")
    default void onTimelineChanged(Timeline timeline, @TimelineChangeReason int reason) {
      Object manifest = null;
      if (timeline.getWindowCount() == 1) {
        // Legacy behavior was to report the manifest for single window timelines only.
        Timeline.Window window = new Timeline.Window();
        manifest = timeline.getWindow(0, window).manifest;
      }
      // Call deprecated version.
      onTimelineChanged(timeline, manifest, reason);
    }

    /**
     * Called when the timeline and/or manifest has been refreshed.
     *
     * <p>Note that if the timeline has changed then a position discontinuity may also have
     * occurred. For example, the current period index may have changed as a result of periods being
     * added or removed from the timeline. This will <em>not</em> be reported via a separate call to
     * {@link #onPositionDiscontinuity(int)}.
     *
     * @param timeline The latest timeline. Never null, but may be empty.
     * @param manifest The latest manifest in case the timeline has a single window only. Always
     *     null if the timeline has more than a single window.
     * @param reason The {@link TimelineChangeReason} responsible for this timeline change.
     * @deprecated Use {@link #onTimelineChanged(Timeline, int)} instead. The manifest can be
     *     accessed by using {@link #getCurrentManifest()} or {@code timeline.getWindow(windowIndex,
     *     window).manifest} for a given window index.
     */
    @Deprecated
    default void onTimelineChanged(
        Timeline timeline, @Nullable Object manifest, @TimelineChangeReason int reason) {}

    /**
     * Called when playback transitions to a media item or starts repeating a media item according
     * to the current {@link #getRepeatMode() repeat mode}.
     *
     * <p>Note that this callback is also called when the playlist becomes non-empty or empty as a
     * consequence of a playlist change.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param mediaItem The {@link MediaItem}. May be null if the playlist becomes empty.
     * @param reason The reason for the transition.
     */
    default void onMediaItemTransition(
        @Nullable MediaItem mediaItem, @MediaItemTransitionReason int reason) {}

    /**
     * Called when the available or selected tracks change.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param trackGroups The available tracks. Never null, but may be of length zero.
     * @param trackSelections The track selections for each renderer. Never null and always of
     *     length {@link #getRendererCount()}, but may contain null elements.
     */
    default void onTracksChanged(
        TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {}

    /**
     * Called when the static metadata changes.
     *
     * <p>The provided {@code metadataList} is an immutable list of {@link Metadata} instances,
     * where the elements correspond to the {@link #getCurrentTrackSelections() current track
     * selections}, or an empty list if there are no track selections or the selected tracks contain
     * no static metadata.
     *
     * <p>The metadata is considered static in the sense that it comes from the tracks' declared
     * Formats, rather than being timed (or dynamic) metadata, which is represented within a
     * metadata track.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param metadataList The static metadata.
     */
    default void onStaticMetadataChanged(List<Metadata> metadataList) {}

    /**
     * Called when the player starts or stops loading the source.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param isLoading Whether the source is currently being loaded.
     */
    @SuppressWarnings("deprecation")
    default void onIsLoadingChanged(boolean isLoading) {
      onLoadingChanged(isLoading);
    }

    /** @deprecated Use {@link #onIsLoadingChanged(boolean)} instead. */
    @Deprecated
    default void onLoadingChanged(boolean isLoading) {}

    /**
     * @deprecated Use {@link #onPlaybackStateChanged(int)} and {@link
     *     #onPlayWhenReadyChanged(boolean, int)} instead.
     */
    @Deprecated
    default void onPlayerStateChanged(boolean playWhenReady, @State int playbackState) {}

    /**
     * Called when the value returned from {@link #getPlaybackState()} changes.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param state The new playback {@link State state}.
     */
    default void onPlaybackStateChanged(@State int state) {}

    /**
     * Called when the value returned from {@link #getPlayWhenReady()} changes.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param playWhenReady Whether playback will proceed when ready.
     * @param reason The {@link PlayWhenReadyChangeReason reason} for the change.
     */
    default void onPlayWhenReadyChanged(
        boolean playWhenReady, @PlayWhenReadyChangeReason int reason) {}

    /**
     * Called when the value returned from {@link #getPlaybackSuppressionReason()} changes.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param playbackSuppressionReason The current {@link PlaybackSuppressionReason}.
     */
    default void onPlaybackSuppressionReasonChanged(
        @PlaybackSuppressionReason int playbackSuppressionReason) {}

    /**
     * Called when the value of {@link #isPlaying()} changes.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param isPlaying Whether the player is playing.
     */
    default void onIsPlayingChanged(boolean isPlaying) {}

    /**
     * Called when the value of {@link #getRepeatMode()} changes.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param repeatMode The {@link RepeatMode} used for playback.
     */
    default void onRepeatModeChanged(@RepeatMode int repeatMode) {}

    /**
     * Called when the value of {@link #getShuffleModeEnabled()} changes.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param shuffleModeEnabled Whether shuffling of windows is enabled.
     */
    default void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {}

    /**
     * Called when an error occurs. The playback state will transition to {@link #STATE_IDLE}
     * immediately after this method is called. The player instance can still be used, and {@link
     * #release()} must still be called on the player should it no longer be required.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param error The error.
     */
    default void onPlayerError(ExoPlaybackException error) {}

    /**
     * Called when a position discontinuity occurs without a change to the timeline. A position
     * discontinuity occurs when the current window or period index changes (as a result of playback
     * transitioning from one period in the timeline to the next), or when the playback position
     * jumps within the period currently being played (as a result of a seek being performed, or
     * when the source introduces a discontinuity internally).
     *
     * <p>When a position discontinuity occurs as a result of a change to the timeline this method
     * is <em>not</em> called. {@link #onTimelineChanged(Timeline, int)} is called in this case.
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param reason The {@link DiscontinuityReason} responsible for the discontinuity.
     */
    default void onPositionDiscontinuity(@DiscontinuityReason int reason) {}

    /**
     * 当前播放参数更改时调用。 播放参数可能会因调用{@link #setPlaybackParameters（PlaybackParameters）}而改变，
     *     或者播放器本身可能会对其进行更改（例如，如果音频播放切换为直通或卸载模式，则无法再进行速度调节）。
     *      //{@ link #onEvents（Player，Events）}也将被调用以报告此事件以及同一{@link Looper}消息队列迭代中发生的其他事件。
     * Called when the current playback parameters change. The playback parameters may change due to
     * a call to {@link #setPlaybackParameters(PlaybackParameters)}, or the player itself may change
     * them (for example, if audio playback switches to passthrough or offload mode, where speed
     * adjustment is no longer possible).
     *
     * <p>{@link #onEvents(Player, Events)} will also be called to report this event along with
     * other events that happen in the same {@link Looper} message queue iteration.
     *
     * @param playbackParameters The playback parameters.
     */
    default void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}

    /**
     * @deprecated Seeks are processed without delay. Listen to {@link
     *     #onPositionDiscontinuity(int)} with reason {@link #DISCONTINUITY_REASON_SEEK} instead.
     */
    @Deprecated
    default void onSeekProcessed() {}

    /**
     * Called when the player has started or stopped offload scheduling.
     *
     * <p>If using ExoPlayer, this is done by calling {@code
     * ExoPlayer#experimentalSetOffloadSchedulingEnabled(boolean)}.
     *
     * <p>This method is experimental, and will be renamed or removed in a future release.
     */
    // TODO(b/172315872) Move this method in a new ExoPlayer.EventListener.
    default void onExperimentalOffloadSchedulingEnabledChanged(boolean offloadSchedulingEnabled) {}

    /**
     * Called when the player has started or finished sleeping for offload.
     *
     * <p>This method is experimental, and will be renamed or removed in a future release.
     */
    default void onExperimentalSleepingForOffloadChanged(boolean sleepingForOffload) {}

    /**
     * Called when one or more player states changed.
     *
     * <p>State changes and events that happen within one {@link Looper} message queue iteration are
     * reported together and only after all individual callbacks were triggered.
     *
     * <p>Listeners should prefer this method over individual callbacks in the following cases:
     *
     * <ul>
     *   <li>They intend to trigger the same logic for multiple events (e.g. when updating a UI for
     *       both {@link #onPlaybackStateChanged(int)} and {@link #onPlayWhenReadyChanged(boolean,
     *       int)}).
     *   <li>They need access to the {@link Player} object to trigger further events (e.g. to call
     *       {@link Player#seekTo(long)} after a {@link #onMediaItemTransition(MediaItem, int)}).
     *   <li>They intend to use multiple state values together or in combination with {@link Player}
     *       getter methods. For example using {@link #getCurrentWindowIndex()} with the {@code
     *       timeline} provided in {@link #onTimelineChanged(Timeline, int)} is only safe from
     *       within this method.
     *   <li>They are interested in events that logically happened together (e.g {@link
     *       #onPlaybackStateChanged(int)} to {@link #STATE_BUFFERING} because of {@link
     *       #onMediaItemTransition(MediaItem, int)}).
     * </ul>
     *
     * @param player The {@link Player} whose state changed. Use the getters to obtain the latest
     *     states.
     * @param events The {@link Events} that happened in this iteration, indicating which player
     *     states changed.
     */
    default void onEvents(Player player, Events events) {}
  }

  /**
   * @deprecated Use {@link EventListener} interface directly for selective overrides as all methods
   *     are implemented as no-op default methods.
   */
  @Deprecated
  abstract class DefaultEventListener implements EventListener {

    @Override
    public void onTimelineChanged(Timeline timeline, @TimelineChangeReason int reason) {
      Object manifest = null;
      if (timeline.getWindowCount() == 1) {
        // Legacy behavior was to report the manifest for single window timelines only.
        Timeline.Window window = new Timeline.Window();
        manifest = timeline.getWindow(0, window).manifest;
      }
      // Call deprecated version.
      onTimelineChanged(timeline, manifest, reason);
    }

    @Override
    public void onTimelineChanged(
        Timeline timeline, @Nullable Object manifest, @TimelineChangeReason int reason) {
      // Do nothing.
    }
  }

  /** A set of {@link EventFlags}. */
  final class Events extends MutableFlags {
    /**
     * Returns whether the given event occurred.
     *
     * @param event The {@link EventFlags event}.
     * @return Whether the event occurred.
     */
    @Override
    public boolean contains(@EventFlags int event) {
      // Overridden to add IntDef compiler enforcement and new JavaDoc.
      return super.contains(event);
    }

    /**
     * Returns whether any of the given events occurred.
     *
     * @param events The {@link EventFlags events}.
     * @return Whether any of the events occurred.
     */
    @Override
    public boolean containsAny(@EventFlags int... events) {
      // Overridden to add IntDef compiler enforcement and new JavaDoc.
      return super.containsAny(events);
    }

    /**
     * Returns the {@link EventFlags event} at the given index.
     *
     * <p>Although index-based access is possible, it doesn't imply a particular order of these
     * events.
     *
     * @param index The index. Must be between 0 (inclusive) and {@link #size()} (exclusive).
     * @return The {@link EventFlags event} at the given index.
     */
    @Override
    @EventFlags
    public int get(int index) {
      // Overridden to add IntDef compiler enforcement and new JavaDoc.
      return super.get(index);
    }
  }

  /**
   * Playback state. One of {@link #STATE_IDLE}, {@link #STATE_BUFFERING}, {@link #STATE_READY} or
   * {@link #STATE_ENDED}.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({STATE_IDLE, STATE_BUFFERING, STATE_READY, STATE_ENDED})
  @interface State {}
  /** The player does not have any media to play播放器没有任何媒体可播放. */
  int STATE_IDLE = 1;
  /**
   * The player is not able to immediately play from its current position. This state typically
   * occurs when more data needs to be loaded播放器无法立即从其当前位置播放。 此状态通常在需要加载更多数据时发生.
   */
  int STATE_BUFFERING = 2;
  /**
   * The player is able to immediately play from its current position. The player will be playing if
   * {@link #getPlayWhenReady()} is true, and paused otherwise.可以立即从其当前位置开始播放。 如果{@link #getPlayWhenReady（）}为true，则播放器将在播放，否则暂停。
   */
  int STATE_READY = 3;
  /** The player has finished playing the media播放器已完成播放媒体. */
  int STATE_ENDED = 4;

  /**
   * Reasons for {@link #getPlayWhenReady() playWhenReady} changes. One of {@link
   * #PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST}, {@link
   * #PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS}, {@link
   * #PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY}, {@link
   * #PLAY_WHEN_READY_CHANGE_REASON_REMOTE} or {@link
   * #PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM}.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
    PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
    PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY,
    PLAY_WHEN_READY_CHANGE_REASON_REMOTE,
    PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM
  })
  @interface PlayWhenReadyChangeReason {}
  /** Playback has been started or paused by a call to {@link #setPlayWhenReady(boolean)}. */
  int PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST = 1;
  /** Playback has been paused because of a loss of audio focus. */
  int PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS = 2;
  /** Playback has been paused to avoid becoming noisy. */
  int PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY = 3;
  /** Playback has been started or paused because of a remote change. */
  int PLAY_WHEN_READY_CHANGE_REASON_REMOTE = 4;
  /** Playback has been paused at the end of a media item. */
  int PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM = 5;

  /**
   * Reason why playback is suppressed even though {@link #getPlayWhenReady()} is {@code true}. One
   * of {@link #PLAYBACK_SUPPRESSION_REASON_NONE} or {@link
   * #PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS}.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    PLAYBACK_SUPPRESSION_REASON_NONE,
    PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
  })
  @interface PlaybackSuppressionReason {}
  /** Playback is not suppressed. */
  int PLAYBACK_SUPPRESSION_REASON_NONE = 0;
  /** Playback is suppressed due to transient audio focus loss. */
  int PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS = 1;

  /**
   * Repeat modes for playback. One of {@link #REPEAT_MODE_OFF}, {@link #REPEAT_MODE_ONE} or {@link
   * #REPEAT_MODE_ALL}.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({REPEAT_MODE_OFF, REPEAT_MODE_ONE, REPEAT_MODE_ALL})
  @interface RepeatMode {}
  /**
   * Normal playback without repetition. "Previous" and "Next" actions move to the previous and next
   * windows respectively, and do nothing when there is no previous or next window to move to.
   */
  int REPEAT_MODE_OFF = 0;
  /**
   * Repeats the currently playing window infinitely during ongoing playback. "Previous" and "Next"
   * actions behave as they do in {@link #REPEAT_MODE_OFF}, moving to the previous and next windows
   * respectively, and doing nothing when there is no previous or next window to move to.
   */
  int REPEAT_MODE_ONE = 1;
  /**
   * Repeats the entire timeline infinitely. "Previous" and "Next" actions behave as they do in
   * {@link #REPEAT_MODE_OFF}, but with looping at the ends so that "Previous" when playing the
   * first window will move to the last window, and "Next" when playing the last window will move to
   * the first window.
   */
  int REPEAT_MODE_ALL = 2;

  /**
   * Reasons for position discontinuities. One of {@link #DISCONTINUITY_REASON_PERIOD_TRANSITION},
   * {@link #DISCONTINUITY_REASON_SEEK}, {@link #DISCONTINUITY_REASON_SEEK_ADJUSTMENT}, {@link
   * #DISCONTINUITY_REASON_AD_INSERTION} or {@link #DISCONTINUITY_REASON_INTERNAL}.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    DISCONTINUITY_REASON_PERIOD_TRANSITION,
    DISCONTINUITY_REASON_SEEK,
    DISCONTINUITY_REASON_SEEK_ADJUSTMENT,
    DISCONTINUITY_REASON_AD_INSERTION,
    DISCONTINUITY_REASON_INTERNAL
  })
  @interface DiscontinuityReason {}
  /**
   * Automatic playback transition from one period in the timeline to the next. The period index may
   * be the same as it was before the discontinuity in case the current period is repeated.
   */
  int DISCONTINUITY_REASON_PERIOD_TRANSITION = 0;
  /** Seek within the current period or to another period. */
  int DISCONTINUITY_REASON_SEEK = 1;
  /**
   * Seek adjustment due to being unable to seek to the requested position or because the seek was
   * permitted to be inexact.
   */
  int DISCONTINUITY_REASON_SEEK_ADJUSTMENT = 2;
  /** Discontinuity to or from an ad within one period in the timeline. */
  int DISCONTINUITY_REASON_AD_INSERTION = 3;
  /** Discontinuity introduced internally by the source. */
  int DISCONTINUITY_REASON_INTERNAL = 4;

  /**
   * Reasons for timeline changes. One of {@link #TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED} or {@link
   * #TIMELINE_CHANGE_REASON_SOURCE_UPDATE}.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED, TIMELINE_CHANGE_REASON_SOURCE_UPDATE})
  @interface TimelineChangeReason {}
  /** Timeline changed as a result of a change of the playlist items or the order of the items. */
  int TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED = 0;
  /** Timeline changed as a result of a dynamic update introduced by the played media. */
  int TIMELINE_CHANGE_REASON_SOURCE_UPDATE = 1;

  /**
   * Reasons for media item transitions. One of {@link #MEDIA_ITEM_TRANSITION_REASON_REPEAT}, {@link
   * #MEDIA_ITEM_TRANSITION_REASON_AUTO}, {@link #MEDIA_ITEM_TRANSITION_REASON_SEEK} or {@link
   * #MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED}.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    MEDIA_ITEM_TRANSITION_REASON_REPEAT,
    MEDIA_ITEM_TRANSITION_REASON_AUTO,
    MEDIA_ITEM_TRANSITION_REASON_SEEK,
    MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
  })
  @interface MediaItemTransitionReason {}
  /** The media item has been repeated. */
  int MEDIA_ITEM_TRANSITION_REASON_REPEAT = 0;
  /** Playback has automatically transitioned to the next media item. */
  int MEDIA_ITEM_TRANSITION_REASON_AUTO = 1;
  /** A seek to another media item has occurred. */
  int MEDIA_ITEM_TRANSITION_REASON_SEEK = 2;
  /**
   * The current media item has changed because of a change in the playlist. This can either be if
   * the media item previously being played has been removed, or when the playlist becomes non-empty
   * after being empty.
   */
  int MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED = 3;

  /**
   * Events that can be reported via {@link EventListener#onEvents(Player, Events)}.
   *
   * <p>One of the {@link Player}{@code .EVENT_*} flags.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    EVENT_TIMELINE_CHANGED,
    EVENT_MEDIA_ITEM_TRANSITION,
    EVENT_TRACKS_CHANGED,
    EVENT_STATIC_METADATA_CHANGED,
    EVENT_IS_LOADING_CHANGED,
    EVENT_PLAYBACK_STATE_CHANGED,
    EVENT_PLAY_WHEN_READY_CHANGED,
    EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED,
    EVENT_IS_PLAYING_CHANGED,
    EVENT_REPEAT_MODE_CHANGED,
    EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
    EVENT_PLAYER_ERROR,
    EVENT_POSITION_DISCONTINUITY,
    EVENT_PLAYBACK_PARAMETERS_CHANGED
  })
  @interface EventFlags {}
  /** {@link #getCurrentTimeline()} changed. */
  int EVENT_TIMELINE_CHANGED = 0;
  /** {@link #getCurrentMediaItem()} changed or the player started repeating the current item. */
  int EVENT_MEDIA_ITEM_TRANSITION = 1;
  /** {@link #getCurrentTrackGroups()} or {@link #getCurrentTrackSelections()} changed. */
  int EVENT_TRACKS_CHANGED = 2;
  /** {@link #getCurrentStaticMetadata()} changed. */
  int EVENT_STATIC_METADATA_CHANGED = 3;
  /** {@link #isLoading()} ()} changed. */
  int EVENT_IS_LOADING_CHANGED = 4;
  /** {@link #getPlaybackState()} changed. */
  int EVENT_PLAYBACK_STATE_CHANGED = 5;
  /** {@link #getPlayWhenReady()} changed. */
  int EVENT_PLAY_WHEN_READY_CHANGED = 6;
  /** {@link #getPlaybackSuppressionReason()} changed. */
  int EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED = 7;
  /** {@link #isPlaying()} changed. */
  int EVENT_IS_PLAYING_CHANGED = 8;
  /** {@link #getRepeatMode()} changed. */
  int EVENT_REPEAT_MODE_CHANGED = 9;
  /** {@link #getShuffleModeEnabled()} changed. */
  int EVENT_SHUFFLE_MODE_ENABLED_CHANGED = 10;
  /** {@link #getPlayerError()} changed. */
  int EVENT_PLAYER_ERROR = 11;
  /** A position discontinuity occurred. See {@link EventListener#onPositionDiscontinuity(int)}. */
  int EVENT_POSITION_DISCONTINUITY = 12;
  /** {@link #getPlaybackParameters()} changed. */
  int EVENT_PLAYBACK_PARAMETERS_CHANGED = 13;

  /** Returns the component of this player for audio output, or null if audio is not supported. */
  @Nullable
  AudioComponent getAudioComponent();

  /** Returns the component of this player for video output, or null if video is not supported. */
  @Nullable
  VideoComponent getVideoComponent();

  /** Returns the component of this player for text output, or null if text is not supported. */
  @Nullable
  TextComponent getTextComponent();

  /**
   * Returns the component of this player for metadata output, or null if metadata is not supported.
   */
  @Nullable
  MetadataComponent getMetadataComponent();

  /** Returns the component of this player for playback device, or null if it's not supported. */
  @Nullable
  DeviceComponent getDeviceComponent();

  /**
   * Returns the {@link Looper} associated with the application thread that's used to access the
   * player and on which player events are received.
   */
  Looper getApplicationLooper();

  /**
   * Register a listener to receive events from the player. The listener's methods will be called on
   * the thread that was used to construct the player. However, if the thread used to construct the
   * player does not have a {@link Looper}, then the listener will be called on the main thread.
   *
   * @param listener The listener to register.
   */
  void addListener(EventListener listener);

  /**
   * Unregister a listener. The listener will no longer receive events from the player.
   *
   * @param listener The listener to unregister.
   */
  void removeListener(EventListener listener);

  /**
   * Clears the playlist, adds the specified {@link MediaItem MediaItems} and resets the position to
   * the default position.
   *
   * @param mediaItems The new {@link MediaItem MediaItems}.
   */
  void setMediaItems(List<MediaItem> mediaItems);

  /**
   * Clears the playlist and adds the specified {@link MediaItem MediaItems}.
   *
   * @param mediaItems The new {@link MediaItem MediaItems}.
   * @param resetPosition Whether the playback position should be reset to the default position in
   *     the first {@link Timeline.Window}. If false, playback will start from the position defined
   *     by {@link #getCurrentWindowIndex()} and {@link #getCurrentPosition()}.
   */
  void setMediaItems(List<MediaItem> mediaItems, boolean resetPosition);

  /**
   * Clears the playlist and adds the specified {@link MediaItem MediaItems}.
   *
   * @param mediaItems The new {@link MediaItem MediaItems}.
   * @param startWindowIndex The window index to start playback from. If {@link C#INDEX_UNSET} is
   *     passed, the current position is not reset.
   * @param startPositionMs The position in milliseconds to start playback from. If {@link
   *     C#TIME_UNSET} is passed, the default position of the given window is used. In any case, if
   *     {@code startWindowIndex} is set to {@link C#INDEX_UNSET}, this parameter is ignored and the
   *     position is not reset at all.
   * @throws IllegalSeekPositionException If the provided {@code windowIndex} is not within the
   *     bounds of the list of media items.
   */
  void setMediaItems(List<MediaItem> mediaItems, int startWindowIndex, long startPositionMs);

  /**
   * Clears the playlist, adds the specified {@link MediaItem} and resets the position to the
   * default position.
   *
   * @param mediaItem The new {@link MediaItem}.
   */
  void setMediaItem(MediaItem mediaItem);

  /**
   * Clears the playlist and adds the specified {@link MediaItem}.
   *
   * @param mediaItem The new {@link MediaItem}.
   * @param startPositionMs The position in milliseconds to start playback from.
   */
  void setMediaItem(MediaItem mediaItem, long startPositionMs);

  /**
   * Clears the playlist and adds the specified {@link MediaItem}.
   *
   * @param mediaItem The new {@link MediaItem}.
   * @param resetPosition Whether the playback position should be reset to the default position. If
   *     false, playback will start from the position defined by {@link #getCurrentWindowIndex()}
   *     and {@link #getCurrentPosition()}.
   */
  void setMediaItem(MediaItem mediaItem, boolean resetPosition);

  /**
   * Adds a media item to the end of the playlist.
   *
   * @param mediaItem The {@link MediaItem} to add.
   */
  void addMediaItem(MediaItem mediaItem);

  /**
   * Adds a media item at the given index of the playlist.
   *
   * @param index The index at which to add the item.
   * @param mediaItem The {@link MediaItem} to add.
   */
  void addMediaItem(int index, MediaItem mediaItem);

  /**
   * Adds a list of media items to the end of the playlist.
   *
   * @param mediaItems The {@link MediaItem MediaItems} to add.
   */
  void addMediaItems(List<MediaItem> mediaItems);

  /**
   * Adds a list of media items at the given index of the playlist.
   *
   * @param index The index at which to add the media items.
   * @param mediaItems The {@link MediaItem MediaItems} to add.
   */
  void addMediaItems(int index, List<MediaItem> mediaItems);

  /**
   * Moves the media item at the current index to the new index.
   *
   * @param currentIndex The current index of the media item to move.
   * @param newIndex The new index of the media item. If the new index is larger than the size of
   *     the playlist the item is moved to the end of the playlist.
   */
  void moveMediaItem(int currentIndex, int newIndex);

  /**
   * Moves the media item range to the new index.
   *
   * @param fromIndex The start of the range to move.
   * @param toIndex The first item not to be included in the range (exclusive).
   * @param newIndex The new index of the first media item of the range. If the new index is larger
   *     than the size of the remaining playlist after removing the range, the range is moved to the
   *     end of the playlist.
   */
  void moveMediaItems(int fromIndex, int toIndex, int newIndex);

  /**
   * Removes the media item at the given index of the playlist.
   *
   * @param index The index at which to remove the media item.
   */
  void removeMediaItem(int index);

  /**
   * Removes a range of media items from the playlist.
   *
   * @param fromIndex The index at which to start removing media items.
   * @param toIndex The index of the first item to be kept (exclusive).
   */
  void removeMediaItems(int fromIndex, int toIndex);

  /** Clears the playlist. */
  void clearMediaItems();

  /** Prepares the player. */
  void prepare();

  /**
   * Returns the current {@link State playback state} of the player.
   *
   * @return The current {@link State playback state}.
   * @see EventListener#onPlaybackStateChanged(int)
   */
  @State
  int getPlaybackState();

  /**
   * Returns the reason why playback is suppressed even though {@link #getPlayWhenReady()} is {@code
   * true}, or {@link #PLAYBACK_SUPPRESSION_REASON_NONE} if playback is not suppressed.
   *
   * @return The current {@link PlaybackSuppressionReason playback suppression reason}.
   * @see EventListener#onPlaybackSuppressionReasonChanged(int)
   */
  @PlaybackSuppressionReason
  int getPlaybackSuppressionReason();

  /**
   * Returns whether the player is playing, i.e. {@link #getContentPosition()} is advancing.
   *
   * <p>If {@code false}, then at least one of the following is true:
   *
   * <ul>
   *   <li>The {@link #getPlaybackState() playback state} is not {@link #STATE_READY ready}.
   *   <li>There is no {@link #getPlayWhenReady() intention to play}.
   *   <li>Playback is {@link #getPlaybackSuppressionReason() suppressed for other reasons}.
   * </ul>
   *
   * @return Whether the player is playing.
   * @see EventListener#onIsPlayingChanged(boolean)
   */
  boolean isPlaying();

  /**
   * Returns the error that caused playback to fail. This is the same error that will have been
   * reported via {@link Player.EventListener#onPlayerError(ExoPlaybackException)} at the time of
   * failure. It can be queried using this method until the player is re-prepared.
   *
   * <p>Note that this method will always return {@code null} if {@link #getPlaybackState()} is not
   * {@link #STATE_IDLE}.
   *
   * @return The error, or {@code null}.
   * @see EventListener#onPlayerError(ExoPlaybackException)
   */
  @Nullable
  ExoPlaybackException getPlayerError();

  /** @deprecated Use {@link #getPlayerError()} instead. */
  @Deprecated
  @Nullable
  ExoPlaybackException getPlaybackError();

  /**
   * Resumes playback as soon as {@link #getPlaybackState()} == {@link #STATE_READY}. Equivalent to
   * {@code setPlayWhenReady(true)}.
   */
  void play();

  /** Pauses playback. Equivalent to {@code setPlayWhenReady(false)}. */
  void pause();

  /**
   * Sets whether playback should proceed when {@link #getPlaybackState()} == {@link #STATE_READY}.
   *
   * <p>If the player is already in the ready state then this method pauses and resumes playback.
   *
   * @param playWhenReady Whether playback should proceed when ready.
   */
  void setPlayWhenReady(boolean playWhenReady);

  /**
   * Whether playback will proceed when {@link #getPlaybackState()} == {@link #STATE_READY}.
   *
   * @return Whether playback will proceed when ready.
   * @see EventListener#onPlayWhenReadyChanged(boolean, int)
   */
  boolean getPlayWhenReady();

  /**
   * Sets the {@link RepeatMode} to be used for playback.
   *
   * @param repeatMode The repeat mode.
   */
  void setRepeatMode(@RepeatMode int repeatMode);

  /**
   * Returns the current {@link RepeatMode} used for playback.
   *
   * @return The current repeat mode.
   * @see EventListener#onRepeatModeChanged(int)
   */
  @RepeatMode
  int getRepeatMode();

  /**
   * Sets whether shuffling of windows is enabled.
   *
   * @param shuffleModeEnabled Whether shuffling is enabled.
   */
  void setShuffleModeEnabled(boolean shuffleModeEnabled);

  /**
   * Returns whether shuffling of windows is enabled.
   *
   * @see EventListener#onShuffleModeEnabledChanged(boolean)
   */
  boolean getShuffleModeEnabled();

  /**
   * Whether the player is currently loading the source.
   *
   * @return Whether the player is currently loading the source.
   * @see EventListener#onIsLoadingChanged(boolean)
   */
  boolean isLoading();

  /**
   * Seeks to the default position associated with the current window. The position can depend on
   * the type of media being played. For live streams it will typically be the live edge of the
   * window. For other streams it will typically be the start of the window.
   */
  void seekToDefaultPosition();

  /**
   * Seeks to the default position associated with the specified window. The position can depend on
   * the type of media being played. For live streams it will typically be the live edge of the
   * window. For other streams it will typically be the start of the window.
   *
   * @param windowIndex The index of the window whose associated default position should be seeked
   *     to.
   * @throws IllegalSeekPositionException If the player has a non-empty timeline and the provided
   *     {@code windowIndex} is not within the bounds of the current timeline.
   */
  void seekToDefaultPosition(int windowIndex);

  /**
   * Seeks to a position specified in milliseconds in the current window.
   *
   * @param positionMs The seek position in the current window, or {@link C#TIME_UNSET} to seek to
   *     the window's default position.
   */
  void seekTo(long positionMs);

  /**
   * Seeks to a position specified in milliseconds in the specified window.
   *
   * @param windowIndex The index of the window.
   * @param positionMs The seek position in the specified window, or {@link C#TIME_UNSET} to seek to
   *     the window's default position.
   * @throws IllegalSeekPositionException If the player has a non-empty timeline and the provided
   *     {@code windowIndex} is not within the bounds of the current timeline.
   */
  void seekTo(int windowIndex, long positionMs);

  /**
   * Returns whether a previous window exists, which may depend on the current repeat mode and
   * whether shuffle mode is enabled.
   *
   * <p>Note: When the repeat mode is {@link #REPEAT_MODE_ONE}, this method behaves the same as when
   * the current repeat mode is {@link #REPEAT_MODE_OFF}. See {@link #REPEAT_MODE_ONE} for more
   * details.
   */
  boolean hasPrevious();

  /**
   * Seeks to the default position of the previous window, which may depend on the current repeat
   * mode and whether shuffle mode is enabled. Does nothing if {@link #hasPrevious()} is {@code
   * false}.
   *
   * <p>Note: When the repeat mode is {@link #REPEAT_MODE_ONE}, this method behaves the same as when
   * the current repeat mode is {@link #REPEAT_MODE_OFF}. See {@link #REPEAT_MODE_ONE} for more
   * details.
   */
  void previous();

  /**
   * Returns whether a next window exists, which may depend on the current repeat mode and whether
   * shuffle mode is enabled.
   *
   * <p>Note: When the repeat mode is {@link #REPEAT_MODE_ONE}, this method behaves the same as when
   * the current repeat mode is {@link #REPEAT_MODE_OFF}. See {@link #REPEAT_MODE_ONE} for more
   * details.
   */
  boolean hasNext();

  /**
   * Seeks to the default position of the next window, which may depend on the current repeat mode
   * and whether shuffle mode is enabled. Does nothing if {@link #hasNext()} is {@code false}.
   *
   * <p>Note: When the repeat mode is {@link #REPEAT_MODE_ONE}, this method behaves the same as when
   * the current repeat mode is {@link #REPEAT_MODE_OFF}. See {@link #REPEAT_MODE_ONE} for more
   * details.
   */
  void next();

  /**
   * Attempts to set the playback parameters. Passing {@code null} sets the parameters to the
   * default, {@link PlaybackParameters#DEFAULT}, which means there is no speed or pitch adjustment.
   *
   * <p>Playback parameters changes may cause the player to buffer. {@link
   * EventListener#onPlaybackParametersChanged(PlaybackParameters)} will be called whenever the
   * currently active playback parameters change.
   *
   * @param playbackParameters The playback parameters, or {@code null} to use the defaults.
   */
  void setPlaybackParameters(@Nullable PlaybackParameters playbackParameters);

  /**
   * Returns the currently active playback parameters.
   *
   * @see EventListener#onPlaybackParametersChanged(PlaybackParameters)
   */
  PlaybackParameters getPlaybackParameters();

  /**
   * Stops playback without resetting the player. Use {@link #pause()} rather than this method if
   * the intention is to pause playback.
   *
   * <p>Calling this method will cause the playback state to transition to {@link #STATE_IDLE}. The
   * player instance can still be used, and {@link #release()} must still be called on the player if
   * it's no longer required.
   *
   * <p>Calling this method does not clear the playlist, reset the playback position or the playback
   * error.
   */
  void stop();

  /**
   * @deprecated Use {@link #stop()} and {@link #clearMediaItems()} (if {@code reset} is true) or
   *     just {@link #stop()} (if {@code reset} is false). Any player error will be cleared when
   *     {@link #prepare() re-preparing} the player.
   */
  @Deprecated
  void stop(boolean reset);

  /**
   * Releases the player. This method must be called when the player is no longer required. The
   * player must not be used after calling this method.
   */
  void release();

  /** Returns the number of renderers. */
  int getRendererCount();

  /**
   * Returns the track type that the renderer at a given index handles.
   *
   * <p>For example, a video renderer will return {@link C#TRACK_TYPE_VIDEO}, an audio renderer will
   * return {@link C#TRACK_TYPE_AUDIO} and a text renderer will return {@link C#TRACK_TYPE_TEXT}.
   *
   * @param index The index of the renderer.
   * @return One of the {@code TRACK_TYPE_*} constants defined in {@link C}.
   */
  int getRendererType(int index);

  /**
   * Returns the available track groups.
   *
   * @see EventListener#onTracksChanged(TrackGroupArray, TrackSelectionArray)
   */
  TrackGroupArray getCurrentTrackGroups();

  /**
   * Returns the current track selections for each renderer.
   *
   * <p>A concrete implementation may include null elements if it has a fixed number of renderer
   * components, wishes to report a TrackSelection for each of them, and has one or more renderer
   * components that is not assigned any selected tracks.
   */
  TrackSelectionArray getCurrentTrackSelections();

  /**
   * Returns the current static metadata for the track selections.
   *
   * <p>The returned {@code metadataList} is an immutable list of {@link Metadata} instances, where
   * the elements correspond to the {@link #getCurrentTrackSelections() current track selections},
   * or an empty list if there are no track selections or the selected tracks contain no static
   * metadata.
   *
   * <p>This metadata is considered static in that it comes from the tracks' declared Formats,
   * rather than being timed (or dynamic) metadata, which is represented within a metadata track.
   *
   * @see EventListener#onStaticMetadataChanged(List)
   */
  List<Metadata> getCurrentStaticMetadata();

  /**
   * Returns the current manifest. The type depends on the type of media being played. May be null.
   */
  @Nullable
  Object getCurrentManifest();

  /**
   * Returns the current {@link Timeline}. Never null, but may be empty.
   *
   * @see EventListener#onTimelineChanged(Timeline, int)
   */
  Timeline getCurrentTimeline();

  /** Returns the index of the period currently being played. */
  int getCurrentPeriodIndex();

  /**
   * Returns the index of the current {@link Timeline.Window window} in the {@link
   * #getCurrentTimeline() timeline}, or the prospective window index if the {@link
   * #getCurrentTimeline() current timeline} is empty.
   */
  int getCurrentWindowIndex();

  /**
   * Returns the index of the window that will be played if {@link #next()} is called, which may
   * depend on the current repeat mode and whether shuffle mode is enabled. Returns {@link
   * C#INDEX_UNSET} if {@link #hasNext()} is {@code false}.
   *
   * <p>Note: When the repeat mode is {@link #REPEAT_MODE_ONE}, this method behaves the same as when
   * the current repeat mode is {@link #REPEAT_MODE_OFF}. See {@link #REPEAT_MODE_ONE} for more
   * details.
   */
  int getNextWindowIndex();

  /**
   * Returns the index of the window that will be played if {@link #previous()} is called, which may
   * depend on the current repeat mode and whether shuffle mode is enabled. Returns {@link
   * C#INDEX_UNSET} if {@link #hasPrevious()} is {@code false}.
   *
   * <p>Note: When the repeat mode is {@link #REPEAT_MODE_ONE}, this method behaves the same as when
   * the current repeat mode is {@link #REPEAT_MODE_OFF}. See {@link #REPEAT_MODE_ONE} for more
   * details.
   */
  int getPreviousWindowIndex();

  /**
   * @deprecated Use {@link #getCurrentMediaItem()} and {@link MediaItem.PlaybackProperties#tag}
   *     instead.
   */
  @Deprecated
  @Nullable
  Object getCurrentTag();

  /**
   * Returns the media item of the current window in the timeline. May be null if the timeline is
   * empty.
   *
   * @see EventListener#onMediaItemTransition(MediaItem, int)
   */
  @Nullable
  MediaItem getCurrentMediaItem();

  /** Returns the number of {@link MediaItem media items} in the playlist. */
  int getMediaItemCount();

  /** Returns the {@link MediaItem} at the given index. */
  MediaItem getMediaItemAt(int index);

  /**
   * Returns the duration of the current content window or ad in milliseconds, or {@link
   * C#TIME_UNSET} if the duration is not known.
   */
  long getDuration();

  /**
   * Returns the playback position in the current content window or ad, in milliseconds, or the
   * prospective position in milliseconds if the {@link #getCurrentTimeline() current timeline} is
   * empty.
   */
  long getCurrentPosition();

  /**
   * Returns an estimate of the position in the current content window or ad up to which data is
   * buffered, in milliseconds.
   */
  long getBufferedPosition();

  /**
   * Returns an estimate of the percentage in the current content window or ad up to which data is
   * buffered, or 0 if no estimate is available.
   */
  int getBufferedPercentage();

  /**
   * Returns an estimate of the total buffered duration from the current position, in milliseconds.
   * This includes pre-buffered data for subsequent ads and windows.
   */
  long getTotalBufferedDuration();

  /**
   * Returns whether the current window is dynamic, or {@code false} if the {@link Timeline} is
   * empty.
   *
   * @see Timeline.Window#isDynamic
   */
  boolean isCurrentWindowDynamic();

  /**
   * Returns whether the current window is live, or {@code false} if the {@link Timeline} is empty.
   *
   * @see Timeline.Window#isLive()
   */
  boolean isCurrentWindowLive();

  /**
   * Returns the offset of the current playback position from the live edge in milliseconds, or
   * {@link C#TIME_UNSET} if the current window {@link #isCurrentWindowLive() isn't live} or the
   * offset is unknown.
   *
   * <p>The offset is calculated as {@code currentTime - playbackPosition}, so should usually be
   * positive.
   *
   * <p>Note that this offset may rely on an accurate local time, so this method may return an
   * incorrect value if the difference between system clock and server clock is unknown.
   */
  long getCurrentLiveOffset();

  /**
   * Returns whether the current window is seekable, or {@code false} if the {@link Timeline} is
   * empty.
   *
   * @see Timeline.Window#isSeekable
   */
  boolean isCurrentWindowSeekable();

  /** Returns whether the player is currently playing an ad. */
  boolean isPlayingAd();

  /**
   * If {@link #isPlayingAd()} returns true, returns the index of the ad group in the period
   * currently being played. Returns {@link C#INDEX_UNSET} otherwise.
   */
  int getCurrentAdGroupIndex();

  /**
   * If {@link #isPlayingAd()} returns true, returns the index of the ad in its ad group. Returns
   * {@link C#INDEX_UNSET} otherwise.
   */
  int getCurrentAdIndexInAdGroup();

  /**
   * If {@link #isPlayingAd()} returns {@code true}, returns the duration of the current content
   * window in milliseconds, or {@link C#TIME_UNSET} if the duration is not known. If there is no ad
   * playing, the returned duration is the same as that returned by {@link #getDuration()}.
   */
  long getContentDuration();

  /**
   * If {@link #isPlayingAd()} returns {@code true}, returns the content position that will be
   * played once all ads in the ad group have finished playing, in milliseconds. If there is no ad
   * playing, the returned position is the same as that returned by {@link #getCurrentPosition()}.
   */
  long getContentPosition();

  /**
   * If {@link #isPlayingAd()} returns {@code true}, returns an estimate of the content position in
   * the current content window up to which data is buffered, in milliseconds. If there is no ad
   * playing, the returned position is the same as that returned by {@link #getBufferedPosition()}.
   */
  long getContentBufferedPosition();
}
