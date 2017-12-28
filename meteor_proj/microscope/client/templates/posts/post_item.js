/**
 * Created by xinqxing on 2017/12/28.
 */
Template.postItem.helpers({ domain: function() {
        var a = document.createElement('a');
        a.href = this.url;
        return a.hostname;
    }
});