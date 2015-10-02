package com.everyplay.android.everyplayrecord;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;
import java.util.Random;

class EveryplayRecordAudioTrack {
    protected int sampleRate = 44100;

    public double stepping(int sampleRateRef) {
        sampleRate = sampleRateRef;
        return 8000.0 / (double) sampleRate;
    }

    public void preSample(int t) {}

    public short sample(int t) {
        return 0;
    }
}

// Simple test case

class EveryplayRecordAudioTrack1 extends EveryplayRecordAudioTrack {
    private int amp = 10000;
    private double twopi = 8. * Math.atan(1.);
    private double fr = 440.f;
    private double ph = 0.0;

    @Override
    public void preSample(int t) {
        fr = 140 + 440 * Math.cos(t);
    }

    @Override
    public short sample(int t) {
        short sample = (short) (amp * Math.sin(ph));

        ph += twopi * fr / sampleRate;
        return sample;
    }
}

// Tracks 2-5 based on work developed at
// http://countercomplex.blogspot.fi/2011/10/algorithmic-symphonies-from-one-line-of.html
// http://www.pouet.net/topic.php?which=8357&page=9

// Original song by tejeez
class EveryplayRecordAudioTrack2 extends EveryplayRecordAudioTrack {
    @Override
    public double stepping(int sampleRateRef) {
        double step = super.stepping(sampleRateRef);

        return step / 2.0;
    }

    @Override
    public short sample(int t) {
        byte sample = (byte) (t * (t >> 11 & t >> 8 & 123 & t >> 3));

        return (short) ((sample - 0x80) << 8);
    }
}

// Original song by tejeez
class EveryplayRecordAudioTrack3 extends EveryplayRecordAudioTrack {
    @Override
    public double stepping(int sampleRateRef) {
        double step = super.stepping(sampleRateRef);

        return step / 3.0;
    }

    @Override
    public short sample(int t) {
        byte sample = (byte) ((byte) ((-t & 4095) * (255 & t * (t & t >> 13)) >> 12) + (127 & t * (234 & t >> 8 & t >> 3) >> (3 & t >> 14)));

        return (short) ((sample - 0x80) << 8);
    }
}

// Original song by viznut
class EveryplayRecordAudioTrack4 extends EveryplayRecordAudioTrack {
    @Override
    public short sample(int t) {
        byte sample = (byte) ((t * 5 & t >> 7) | (t * 3 & t >> 10));

        return (short) ((sample - 0x80) << 8);
    }
}

// Original song "Longline Theory" by veqtor, human readable version by Eiyeron
class EveryplayRecordAudioTrack5 extends EveryplayRecordAudioTrack {
    private double sb, y, h, a, d, g;

    private int backgroundWaveNotes[] = {15, 15, 23, 8};
    private double mainInstrumentNotes[][] = { {15, 18, 17, 17, 17, 17, 999, 999, 22, 22, 999, 18, 999, 15, 20, 22},
                                               {20, 18, 17, 17, 10, 10, 999, 999, 20, 22, 20, 18, 17, 18, 17, 10}};

    @Override
    public short sample(int t) {
        sb = (t > 0xffff ? 1 : 0);

        y = Math.pow(2, backgroundWaveNotes[t >> 14 & 3] / 12.);

        a = 1. - ((t & 0x7ff) / (double) 0x7ff);
        d = (((int) 14. * t * t ^ t) & 0x7ff);

        g = (double) (t & 0x7ff) / (double) 0x7ff;
        g = 1. - (g * g);

        h = Math.pow(2., mainInstrumentNotes[((t >> 14 & 3) > 2 ? 1 : 0) & 1][t >> 10 & 15] / 12);

        double wave = ((int) (y * t * 0.241) & 127 - 64) + ((int) (y * t * 0.25) & 127 - 64) * 1.2;
        double drum = (((int) (((int) (5. * t) & 0x7ff) * a) & 255 - 127) * ((0x53232323 >> (t >> 11 & 31)) & 1) * a * 1.0

                       + ((int) (d * a) & 255 - 128) * ((0xa444c444 >> (t >> 11 & 31)) & 1) * a * 1.5 + ((int) ((a * a * d * (t >> 9 & 1))) & 0xff - 0x80) * 0.1337)
            * sb;

        double instrument = (((int) (h * t) & 31) + ((int) (h * t * 1.992) & 31) + ((int) (h * t * .497) & 31) + ((int) (h * t * 0.977) & 31))
            * g * sb;

        byte sample = (byte) Math.max(Math.min(((wave + drum + instrument) / 3.), 127), -128);
        return (short) ((sample - 0x80) << 8);
    }
}

// Original C synth code by Ector/Deus Ex Machina, modified by tonic & !Cube
// Song by !Cube for San Angeles Observation, a 4k intro (1st at Assembly 2004)
// Graphics part later used in numerous graphics examples, incl. Android NDK/WebGL
//
// http://www.pouet.net/prod.php?which=13020
//
// Quick port to Java by aet (at) unity3d.com
class EveryplayRecordAudioTrack6 extends EveryplayRecordAudioTrack {
    @Override
    public double stepping(int sampleRateRef) {
        sampleRate = sampleRateRef;
        return 44100.0 / (double) sampleRate;
    }

    // Channel
    class MelodyChannel {
        public MelodyChannel(short cutoffArg, short resonanceArg, byte transposeArg, byte lfoArg, byte typeArg, byte envShiftArg,
                             byte envShift2Arg, byte detuneArg, int volumeOrigArg, int volumeArg, int volumeFadeArg, double vibSpeedArg,
                             double vibRangeArg, int initPos2Arg) {
            this.cutoff = cutoffArg;
            this.resonance = resonanceArg;
            this.transpose = transposeArg;
            this.lfo = lfoArg;
            this.type = typeArg;
            this.envShift = envShiftArg;
            this.envShift2 = envShift2Arg;
            this.detune = detuneArg;

            this.volumeOrig = volumeOrigArg;
            this.volume = volumeArg;
            this.volumeFade = volumeFadeArg;

            this.vibSpeed = vibSpeedArg;
            this.vibRange = vibRangeArg;
            this.initPos2 = initPos2Arg;

            this.pos = 0;
            this.delta = 0;
            this.pos2 = 0;
            this.delta2 = 0;

            this.envPos = 0;
            this.filterPos = 0;
            this.filterDelta = 0;

            this.note = 0;
        }

        // Filter properties
        short cutoff;
        short resonance;
        byte transpose; // transpose in octaves
        byte lfo; // filter sweep
        byte type; // saw/square wave (bits 0,1)
        byte envShift;
        byte envShift2;
        byte detune;

        int volumeOrig;
        int volume;
        int volumeFade;

        double vibSpeed;
        double vibRange;

        int initPos2; // oscillator2 init position
        int pos; // oscillator position
        long delta; // its speed
        int pos2; // oscillator2 position
        long delta2; // its speed

        int envPos; // for envelope
        int filterPos;
        int filterDelta;

        short note; // 0 = off
    };

    private final int SONGLENGTH = 41;
    @SuppressWarnings("unused")
    private final int NUMPATTERNS = 22, NUMCHANNELS = 9;

    private final int PATTERNLENGTH = 16; // must be power of 2
    private final int PATTERNLENGTH_SHIFT = 4;

    // s = sharp note
    private final short HS = 0x80; // hardsync

    @SuppressWarnings("unused")
    private final short xx = -1, C1 = 0, Cs1 = 1, D1 = 2, Ds1 = 3, E1 = 4, F1 = 5, Fs1 = 6, G1 = 7, Gs1 = 8, A1 = 9, As1 = 10, B1 = 11,
    C2 = 12, Cs2 = 13, D2 = 14, Ds2 = 15, E2 = 16, F2 = 17, Fs2 = 18, G2 = 19, Gs2 = 20, A2 = 21, As2 = 22, B2 = 23,
    C3 = 24, Cs3 = 25, D3 = 26, Ds3 = 27, E3 = 28, F3 = 29, Fs3 = 30, G3 = 31, Gs3 = 32, A3 = 33, As3 = 34, B3 = 35,
    C4 = 36, Cs4 = 37, D4 = 38, Ds4 = 39, E4 = 40, F4 = 41, Fs4 = 42, G4 = 43, Gs4 = 44, A4 = 45, As4 = 46, B4 = 47,
    C5 = 48, Cs5 = 49, D5 = 50, Ds5 = 51, E5 = 52, F5 = 53, Fs5 = 54, G5 = 55, Gs5 = 56, A5 = 57, As5 = 58, B5 = 59,
    C6 = 60, Cs6 = 61, D6 = 62, Ds6 = 63, E6 = 64, F6 = 65, Fs6 = 66, G6 = 67, Gs6 = 68, A6 = 69, As6 = 70, B6 = 71,
    C7 = 72, Cs7 = 73, D7 = 74, Ds7 = 75, E7 = 76, F7 = 77, Fs7 = 78, G7 = 79, Gs7 = 80, A7 = 81, As7 = 82, B7 = 83;

    private final int TEMPO = 7500;
    private final int TICK = (TEMPO / 6);

    private final int LFO_BASE = 0;
    private final double LFO_PHASE = Math.PI;
    private final int LFO_RANGE = 128;
    private final double LFO_SPEED = (2 * Math.PI) / (TEMPO * 32);

    private final int START_CHANNEL = 0;
    private final int END_CHANNEL = 9;

    private final int DELAY_CHANNELS = 4;
    private final int BASSDRUM_CHANNEL = 6;
    private final int SNARE_CHANNEL = 7;
    private final int HIHAT_CHANNEL = 8;
    private final int SHIFT_OPT = (12 - BASSDRUM_CHANNEL);

    private final int DELAYSIZE = (TEMPO * 3);

    private final short PAD_RESO = 100;
    private final int PAD_VOLUME = 180;
    private final int PAD_FADEOUT = 220;

    private final short CHORD_RESO = (short) 240;
    private final int CHORD_VOLUME = 60;
    private final int CHORD_FADEOUT = 80;
    private final byte CHORD_TRANSPOSE = (byte) (3 * 12);
    private final byte CHORD_TYPE = (byte) 1;
    private final byte CHORD_LFO = (byte) 1;
    private final byte CHORD_ENVSHIFT = (byte) 24;
    private final byte CHORD_ENVSHIFT2 = (byte) 17;
    private final byte CHORD_DETUNE = (byte) 4;
    private final int CHORD_INITPOS2 = 0;
    private final double CHORD_VIBSPEED = 1600.0;
    private final double CHORD_VIBRANGE = 400000.0;

    private final MelodyChannel chans[] /* NUMCHANNELS */ = {
        /* PLUCK */
        new MelodyChannel(
            /* cutoff */ (short) 0,
            /* resonance */ (short) 120,
            /* transpose */ (byte) (2 * 12),
            /* lfo */ (byte) 0,
            /* type */ (byte) 1,
            /* envShift */ (byte) 14,
            /* envShift2 */ (byte) 13,
            /* detune */ (byte) 6,
            /* volumeOrig */ 180,
            /* volume */ 0,
            /* volumeFade */ (10 << 8),
            /* vibSpeed */ 1600.0,
            /* vibRange */ 150000.0,
            /* initPos2 */ (8192 << 18)),

        /* CHORD1 */
        new MelodyChannel(
            /* cutoff */ (short) 0,
            /* resonance */ CHORD_RESO,
            /* transpose */ CHORD_TRANSPOSE,
            /* lfo */ CHORD_LFO,
            /* type */ CHORD_TYPE,
            /* envShift */ CHORD_ENVSHIFT,
            /* envShift2 */ CHORD_ENVSHIFT2,
            /* detune */ CHORD_DETUNE,
            /* volumeOrig */ CHORD_VOLUME,
            /* volume */ 0,
            /* volumeFade */ CHORD_FADEOUT,
            /* vibSpeed */ CHORD_VIBSPEED,
            /* vibRange */ CHORD_VIBRANGE,
            /* initPos2 */ CHORD_INITPOS2),

        /* CHORD2 */
        new MelodyChannel(
            /* cutoff */ (short) 0,
            /* resonance */ CHORD_RESO,
            /* transpose */ CHORD_TRANSPOSE,
            /* lfo */ CHORD_LFO,
            /* type */ CHORD_TYPE,
            /* envShift */ CHORD_ENVSHIFT,
            /* envShift2 */ CHORD_ENVSHIFT2,
            /* detune */ CHORD_DETUNE,
            /* volumeOrig */ CHORD_VOLUME,
            /* volume */ 0,
            /* volumeFade */ CHORD_FADEOUT,
            /* vibSpeed */ CHORD_VIBSPEED,
            /* vibRange */ CHORD_VIBRANGE,
            /* initPos2 */ CHORD_INITPOS2),

        /* CHORD3 */
        new MelodyChannel(
            /* cutoff */ (short) 0,
            /* resonance */ CHORD_RESO,
            /* transpose */ CHORD_TRANSPOSE,
            /* lfo */ CHORD_LFO,
            /* type */ CHORD_TYPE,
            /* envShift */ CHORD_ENVSHIFT,
            /* envShift2 */ CHORD_ENVSHIFT2,
            /* detune */ CHORD_DETUNE,
            /* volumeOrig */ CHORD_VOLUME,
            /* volume */ 0,
            /* volumeFade */ CHORD_FADEOUT,
            /* vibSpeed */ CHORD_VIBSPEED,
            /* vibRange */ CHORD_VIBRANGE,
            /* initPos2 */ CHORD_INITPOS2),

        /* PAD OCTAVE 1 */
        new MelodyChannel(
            /* cutoff */ (short) 0,
            /* resonance */ PAD_RESO,
            /* transpose */ (byte) (-1 * 12),
            /* lfo */ (byte) 1,
            /* type */ (byte) 1,
            /* envShift */ (byte) 24,
            /* envShift2 */ (byte) 17,
            /* detune */ (byte) 6,
            /* volumeOrig */ PAD_VOLUME,
            /* volume */ 0,
            /* volumeFade */ PAD_FADEOUT,
            /* vibSpeed */ 1600.0,
            /* vibRange */ 150000.0,
            /* initPos2 */ (8192 << 18)),

        /* PAD OCTAVE 2 */
        new MelodyChannel(
            /* cutoff */ (short) 0,
            /* resonance */ PAD_RESO,
            /* transpose */ (byte) 0,
            /* lfo */ (byte) 1,
            /* type */ (byte) 1,
            /* envShift */ (byte) 24,
            /* envShift2 */ (byte) 17,
            /* detune */ (byte) 6,
            /* volumeOrig */ PAD_VOLUME,
            /* volume */ 0,
            /* volumeFade */ PAD_FADEOUT,
            /* vibSpeed */ 1600.0,
            /* vibRange */ 150000.0,
            /* initPos2 */ (8192 << 18)),

        /* BASSDRUM */
        new MelodyChannel(
            /* cutoff */ (short) 0,
            /* resonance */ (short) 0,
            /* transpose */ (byte) (6 * 12),
            /* lfo */ (byte) 0,
            /* type */ (byte) 1,
            /* envShift */ (byte) 24,
            /* envShift2 */ (byte) 16,
            /* detune */ (byte) 0,
            /* volumeOrig */ 200,
            /* volume */ 0,
            /* volumeFade */ (17 << 8),
            /* vibSpeed */ 0.0,
            /* vibRange */ 0.0,
            /* initPos2 */ (8192 << 18)),

        /* SNARE */
        new MelodyChannel(
            /* cutoff */ (short) 0,
            /* resonance */ (short) 230,
            /* transpose */ (byte) (5 * 12),
            /* lfo */ (byte) 0,
            /* type */ (byte) 1,
            /* envShift */ (byte) 24,
            /* envShift2 */ (byte) 16,
            /* detune */ (byte) 0,
            /* volumeOrig */ 150,
            /* volume */ 0,
            /* volumeFade */ (10 << 8),
            /* vibSpeed */ 0.0,
            /* vibRange */ 0.0,
            /* initPos2 */ (8192 << 18)),

        /* HIHAT */
        new MelodyChannel(
            /* cutoff */ (short) 0,
            /* resonance */ (short) 0,
            /* transpose */ (byte) (7 * 12),
            /* lfo */ (byte) 0,
            /* type */ (byte) 1,
            /* envShift */ (byte) 24,
            /* envShift2 */ (byte) 15,
            /* detune */ (byte) 0,
            /* volumeOrig */ 165,
            /* volume */ 0,
            /* volumeFade */ (60 << 8),
            /* vibSpeed */ 0.0,
            /* vibRange */ 0.0,
            /* initPos2 */ 0),
    };

    private final short L1 = 1, L2 = 2, L3 = 3, L4 = 4, L5 = 5, L6 = 6, L7 = 7, L8 = 8, L9 = 9;
    private final short C_G4 = 10, C_As4 = 11, C_F5 = 12, C_A4 = 13, C_G5 = 14, C_A5 = 15, C_D5 = 16, C_Ds5 = 17;
    private final short D_B1 = 18, D_B2 = 19, D_S1 = 20, D_S2 = 21, D_H = 22;

    // song orders with pattern for each channel
    // time goes horizontally, 0 = no pattern
    private final short seq[] /* NUMCHANNELS */[] /* SONGLENGTH */ = {
        {L1, 0, L2, 0, L2, 0, L3, 0, L4, 0, L5, 0, L2, 0, L3, 0, L4, 0, L5, 0, L2, 0, L3, 0, L4, 0, L5, 0, L6, 0, L7, 0, L8, 0, L9, 0, L1,
         0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, C_G4, 0, C_G4, 0, C_G4, 0, C_G4, 0, C_As4, 0, C_As4, 0, C_As4, 0,
         C_As4, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, C_As4, 0, C_A4, 0, C_As4, 0, C_As4, 0, C_D5, 0, C_D5, 0, C_Ds5, 0,
         C_Ds5, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, C_F5, 0, C_F5, 0, C_G5, 0, C_A5, 0, C_G5, 0, C_F5, 0, C_G5, 0, C_F5,
         0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, C_G5, 0, C_G5, 0, C_Ds5, 0, C_Ds5, 0, C_G5, 0, C_G5, 0, C_Ds5, 0, C_Ds5, 0, C_G5, 0, C_G5, 0, C_Ds5, 0, C_Ds5, 0,
         C_G5, 0, C_G5, 0, C_Ds5, 0, C_Ds5, 0, C_G5, 0, C_G5, 0, 0},
        {0, 0, 0, 0, C_G5, 0, C_G5, 0, C_Ds5, 0, C_Ds5, 0, C_G5, 0, C_G5, 0, C_Ds5, 0, C_Ds5, 0, C_G5, 0, C_G5, 0, C_Ds5, 0, C_Ds5, 0,
         C_G5, 0, C_G5, 0, C_Ds5, 0, C_Ds5, 0, C_G5, 0, C_G5, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, D_B1, D_B1, D_B2, D_B1, D_B1, D_B1, D_B2, D_B1, D_B1, D_B1, D_B2, D_B1, D_B1, D_B1, D_B2,
         D_B1, D_B1, D_B1, D_B2, D_B1, D_B1, D_B1, D_B2, D_B1, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, D_S1, D_S1, D_S1, D_S2, D_S1, D_S1, D_S1, D_S2, D_S1, D_S1, D_S1, D_S2, D_S1, D_S1, D_S1,
         D_S2, D_S1, D_S1, D_S1, D_S2, D_S1, D_S1, D_S1, D_S2, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, D_H, D_H, D_H, D_H, D_H, D_H, D_H, D_H, D_H, D_H, D_H, D_H, D_H, D_H, D_H, D_H, D_H, D_H, D_H,
         D_H, D_H, D_H, D_H, D_H, 0, 0, 0, 0, 0},
    };

    // patterns (xx = no note)

    private final short BASSDRUM = (short) (C1 | HS);
    private final short SNARE = (short) (D2 | HS);
    private final short HIHAT1 = (short) (Cs1 | HS);
    private final short HIHAT2 = (short) (C1 | HS);

    private final short patterns[] /* NUMPATTERNS */[] /* PATTERNLENGTH */ = {
        {(short) (G4 | HS), xx, xx, xx, As4, xx, xx, xx, A4, xx, xx, xx, xx, xx, xx, xx},
        {G4, xx, xx, xx, As4, xx, xx, xx, A4, xx, xx, xx, xx, xx, xx, xx},
        {A4, xx, xx, xx, C5, xx, xx, xx, As4, xx, xx, xx, xx, xx, xx, xx},
        {C5, xx, xx, xx, G5, xx, xx, xx, F5, xx, xx, xx, xx, xx, xx, xx},
        {A5, xx, xx, xx, As5, xx, xx, xx, G5, xx, xx, xx, xx, xx, xx, xx},
        {A5, xx, xx, xx, C6, xx, xx, xx, As5, xx, xx, xx, xx, xx, xx, xx},
        {C6, xx, xx, xx, Ds6, xx, xx, xx, D6, xx, xx, xx, xx, xx, xx, xx},
        {G6, xx, xx, xx, F6, xx, xx, xx, As5, xx, xx, xx, xx, xx, xx, xx},
        {Ds6, xx, xx, xx, D6, xx, xx, xx, G5, xx, xx, xx, xx, xx, xx, xx},

        // chords
        {(short) (G4 | HS), xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx},
        {(short) (As4 | HS), xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx},
        {(short) (F5 | HS), xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx},
        {(short) (A4 | HS), xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx},
        {(short) (G5 | HS), xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx},
        {(short) (A5 | HS), xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx},
        {(short) (D5 | HS), xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx},
        {(short) (Ds5 | HS), xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx, xx},

        // drums
        {BASSDRUM, xx, xx, xx, xx, xx, xx, xx, xx, xx, BASSDRUM, xx, xx, BASSDRUM, xx, xx},
        {BASSDRUM, xx, xx, xx, xx, xx, xx, xx, BASSDRUM, xx, BASSDRUM, xx, xx, BASSDRUM, xx, xx},
        {xx, xx, xx, xx, SNARE, xx, xx, xx, xx, xx, xx, xx, SNARE, xx, xx, xx},
        {xx, xx, xx, xx, SNARE, xx, xx, xx, xx, xx, xx, xx, SNARE, xx, xx, SNARE},
        {HIHAT1, HIHAT2, HIHAT1, HIHAT2, HIHAT1, HIHAT2, HIHAT1, HIHAT2, HIHAT1, HIHAT2, HIHAT1, HIHAT2, HIHAT1, HIHAT2, HIHAT1, HIHAT2},
    };

    private int delayBuffer[] = new int[DELAYSIZE];
    private int n1 = 0, n2 = 0;
    private int delayPos;

    private final Random rand = new Random();
    private short buffer[] = new short[2];

    private int songPos = 0; // song position in 1/16 notes
    private int tempoTime = 0;
    private int tickTime = 0;
    public int syncFlag = 0; // sync flag set to 1 on every 4th row

    private static long getUnsignedInt(int x) {
        return x & (-1L >>> 32);
    }

    private void soundStep() {
        int i;

        for (i = START_CHANNEL; i < END_CHANNEL; ++i) {
            int pat = seq[i][(songPos >> PATTERNLENGTH_SHIFT) % SONGLENGTH]; // current pattern (with looping)

            if (pat != 0) {
                int note = patterns[pat - 1][songPos & (PATTERNLENGTH - 1)]; // current note

                if (note != xx) {
                    chans[i].note = (short) (note & 0x7F);

                    double deltaf = (398126.91177592743764172335600907 * Math.pow(2.0, (chans[i].note + chans[i].transpose) / 12.0));

                    chans[i].delta = getUnsignedInt((int) deltaf);
                    chans[i].delta2 = chans[i].delta + ((chans[i].detune) << 13);
                    int hs = (note & HS);

                    if (hs != 0) {
                        chans[i].pos = 0;
                        chans[i].pos2 = chans[i].initPos2;
                    }
                    chans[i].envPos = 1 << 24;
                    chans[i].volume = (chans[i].volumeOrig << 8);

                    if (!((note & 1) == 1) && (i == HIHAT_CHANNEL)) {
                        chans[i].volume = (chans[i].volume * 5) >> 3;
                    }
                }
            }
        }

        ++songPos;

        // set sync flag on every 4th row
        if (((songPos + 4) & 3) == 0) {
            syncFlag = 1;
        }
    }

    public short sample(int t) {
        int wave = 0, c = 0, v = 0, tp = 0;

        int cutAdd = LFO_BASE + (int) (LFO_RANGE * (Math.cos(LFO_PHASE + t * LFO_SPEED)));

        // rewind
        if (t <= 1) {
            songPos = 0;
            tempoTime = 0;
            tickTime = 0;
            syncFlag = 0;
        }

        if (tempoTime == 0) {
            soundStep(); // step song forward
        }

        for (c = START_CHANNEL; c < END_CHANNEL; ++c) {
            int vibrato = (int) (Math.sin(t / chans[c].vibSpeed) * chans[c].vibRange);

            int sample = (int) (chans[c].pos >> 18);
            int sample2 = (int) (chans[c].pos2 >> 18);
            sample = sample - sample2;

            tp = chans[c].pos >> 26;
            chans[c].pos += chans[c].delta + vibrato; // go forward saw wave
            chans[c].pos2 += chans[c].delta2 + vibrato;

            if (tp != (chans[c].pos >> 26)) {
                if (c == SNARE_CHANNEL) {
                    n1 = rand.nextInt() & 0x3fff;
                } else if (c == HIHAT_CHANNEL) {
                    n2 = rand.nextInt() & 0x3fff;
                }
            }

            chans[c].envPos -= chans[c].envPos >> chans[c].envShift;

            if (c == DELAY_CHANNELS) {
                // do delay effect here so that it will not affect drums
                v = (delayBuffer[delayPos] * 5) >> 3;
                delayBuffer[delayPos] = v + wave;
                ++delayPos;
                if (delayPos >= DELAYSIZE) {
                    delayPos = 0;
                }
            }

            if (c == BASSDRUM_CHANNEL || c == SNARE_CHANNEL) {
                chans[c].delta -= chans[c].delta >> (c + SHIFT_OPT);
                chans[c].delta2 = chans[c].delta;
                if (c == SNARE_CHANNEL) {
                    sample += n1;
                }
            } else if (c == HIHAT_CHANNEL) {
                sample = n2;
            }

            // volume
            sample = (sample * (chans[c].volume >> 8)) >> 8;

            if (tickTime == 0) {
                chans[c].volume -= chans[c].volumeFade;
                if (chans[c].volume < 0) {
                    chans[c].volume = 0;
                }
            }

            {
                int cut = chans[c].cutoff + (chans[c].envPos >> chans[c].envShift2);
                if (chans[c].lfo != 0) {
                    cut += cutAdd;
                }

                if (cut < 10) {
                    cut = 10;
                }

                chans[c].filterDelta = (chans[c].filterDelta * chans[c].resonance) >> 8;
                chans[c].filterDelta += ((sample - chans[c].filterPos) * cut) >> 11;
                chans[c].filterPos += chans[c].filterDelta;
            }

            wave += chans[c].filterPos;

            // chans[c].envPos *= 0.999; // envelope fade out
        }

        {
            boolean surround = false;
            boolean clipping = true;

            // surround sound
            int sample1 = (wave - v) >> 1;
            int sample2 = (wave + v) >> 1;

            if (surround == false) {
                sample1 = sample2;
            }

            if (clipping == true) {
                if (sample1 < -32768) {
                    sample1 = -32768;
                }
                if (sample1 > 32767) {
                    sample1 = 32767;
                }
                if (sample2 < -32768) {
                    sample2 = -32768;
                }
                if (sample2 > 32767) {
                    sample2 = 32767;
                }
            }

            buffer[0] = (short) sample1;
            buffer[1] = (short) sample2;
        }

        if (++tempoTime > TEMPO) {
            tempoTime = 0;
        }
        if (++tickTime > TICK) {
            tickTime = 0;
        }

        // Settle for mono output
        return buffer[0];
    }
}

public class EveryplayRecordAudioGenerator {
    private EveryplayRecordAudioTrack generator = null;
    private Thread t = null;
    private int sampleRate = 44100;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private int buffsize = 0;
    private AudioTrack audioTrack = null;
    private short samples[] = null;
    private double d = 0.0f;

    private static EveryplayRecordAudioTrack mapTrack(int index) {
        EveryplayRecordAudioTrack track = null;

        switch (index) {
        case 1:
            track = new EveryplayRecordAudioTrack1();
            break;
        case 2:
            track = new EveryplayRecordAudioTrack2();
            break;
        case 3:
            track = new EveryplayRecordAudioTrack3();
            break;
        case 4:
            track = new EveryplayRecordAudioTrack4();
            break;
        case 5:
            track = new EveryplayRecordAudioTrack5();
            break;
        case 6:
        default:
            track = new EveryplayRecordAudioTrack6();
            break;
        }
        return track;
    }

    public EveryplayRecordAudioGenerator(int index) {
        this(mapTrack(index));
    }

    public EveryplayRecordAudioGenerator(EveryplayRecordAudioTrack input) {
        // set the buffer size
        buffsize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        // Avoid stuttering
        buffsize *= 2;

        samples = new short[buffsize];

        generator = input;
    }

    public void play() {
        if (isRunning == true) {
            return;
        }
        isRunning = true;

        // start a new thread to synthesise audio
        t = new Thread() {
            public void run() {
                // set process priority
                int THREAD_PRIORITY_AUDIO = -16;

                Process.setThreadPriority(THREAD_PRIORITY_AUDIO);

                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, buffsize, AudioTrack.MODE_STREAM);

                audioTrack.play();

                double step = generator.stepping(sampleRate);

                d = 0.0;
                int t = 0;

                // synthesis loop
                while (isRunning) {
                    if (isPaused == false) {
                        generator.preSample(t);
                        for (int i = 0; i < buffsize; i++) {
                            d += step;
                            t = (int) d;
                            short sample = generator.sample(t);
                            samples[i] = sample;
                        }
                        audioTrack.write(samples, 0, buffsize);
                    }
                }

                audioTrack.stop();
                audioTrack.release();
            }
        };
        t.start();
    }

    public void stop() {
        isRunning = false;
    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
    }

    public void rewind() {
        d = 0.0;
    }
}
