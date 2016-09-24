package org.adridadou.ethereum.ethereumj.config;

/**
 * Created by davidroon on 18.09.16.
 * This code is released under Apache 2 license
 *
 * dump {
 * # for testing purposes
 * # all the state will be dumped
 * # in JSON form to [dump.dir]
 * # if [dump.full] = true
 * # possible values [true/false]
 * full = false
 * dir = dmp
 *
 * # This defines the vmtrace dump
 * # to the console and the style
 * # -1 for no block trace
 * # styles: [pretty/standard+] (default: standard+)
 * block = -1
 * style = pretty
 *
 * # clean the dump dir each start
 * clean.on.restart = true
 * }
 */
public class DumpConfig {
    private final Boolean cleanOnRestart;
    private final VmTraceDumpStyle style;
    private final Integer block;
    private final String dir;
    private final Boolean full;

    public DumpConfig(Boolean cleanOnRestart, VmTraceDumpStyle style, Integer block, String dir, Boolean full) {
        this.cleanOnRestart = cleanOnRestart;
        this.style = style;
        this.block = block;
        this.dir = dir;
        this.full = full;
    }


}