// global configurations and constant objects

COINPORT = {
    priceFixed: {
        'btc-cny': 2,
        'ltc-btc': 4,
        'doge-btc': 8,
        'bc-btc': 8,
        'drk-btc': 6,
        'vrc-btc': 8,
        'zet-btc': 8
    },

    amountFixed: {
        cny: 8,
        btc: 8,
        ltc: 8,
        doge: 8,
        bc: 8,
        drk: 8,
        vrc: 8,
        zet: 8
    },

//    amountFixed: {
//        cny: 4,
//        btc: 4,
//        ltc: 4,
//        doge: 4,
//        bc: 3,
//        drk: 2,
//        vrc: 3,
//        zet: 3
//    },

    defaultMarket: 'LTC-BTC',

    blockUrl: {
        BTC: 'https://blockchain.info/block-index/',
        LTC: 'http://block-explorer.com/block/',
        DOGE: 'http://dogechain.info/block/',
        BC: 'http://blackcha.in/block/',
        DRK: 'http://explorer.darkcoin.io/block/',
        VRC: 'http://blocks.vericoin.info/block/',
        ZET: 'https://coinplorer.com/ZET/Blocks/'
    },

    addressUrl: {
        BTC: 'https://blockchain.info/address/',
        LTC: 'http://block-explorer.com/address/',
        DOGE: 'http://dogechain.info/address/',
        BC: 'http://blackcha.in/address/',
        DRK: 'http://explorer.darkcoin.io/address/',
        VRC: 'http://blocks.vericoin.info/address/',
        ZET: 'https://coinplorer.com/ZET/Addresses/'
    },

    txUrl: {
        BTC: 'https://blockchain.info/tx/',
        LTC: 'http://block-explorer.com/tx/',
        DOGE: 'http://dogechain.info/tx/',
        BC: 'http://blackcha.in/tx/',
        DRK: 'http://explorer.darkcoin.io/tx/',
        VRC: 'http://blocks.vericoin.info/tx/',
        ZET: 'https://coinplorer.com/ZET/Transactions/'
    }
};

COINPORT.getPriceFixed = function(market) {
    return COINPORT.priceFixed[market.toLowerCase()];
};

COINPORT.getAmountFixed = function(currency) {
    return COINPORT.amountFixed[currency.toLowerCase()];
};

COINPORT.floor = function(value, precision) {
    if (isNaN(value))
        return value;

    var s = '' + value;
    var offset = s.indexOf('.');
    if (offset < 0)
        return value;
    offset += precision;
    return s.substring(0, offset + 1);
};

COINPORT.numberRegExp =  /^\d*(\.)?(\d+)*$/;