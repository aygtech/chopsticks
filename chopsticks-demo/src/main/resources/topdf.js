var page = require( 'webpage' ).create();
var oss_url, out_pdf;
var system = require('system')
oss_url = system.args[1];  //输入，系统参数
out_pdf = system.args[2]; //输出， 系统参数

page.viewportSize = { width: 1024, height: 800 };  //viewport size

page.paperSize = {
    format: 'A4',
    orientation: 'portrait',
    margin: '1cm',  //页边距
    header: { //如果不需要，可以不用添加
            height: '1cm',
            contents: phantom.callback(function(pageNum, numPages) {
                   //返回页眉的代码逻辑
                   
            })
    },
    footer: { //如果不需要，可以不用添加
            height: '1cm',
            contents: phantom.callback(function(pageNum, numPages) {
                    //返回页脚的代码逻辑
                    
            })
    }
};
console.log(oss_url);
page.open( oss_url, function( status ) {
    console.log(status)
    if ( status === "success" ) {
        console.log(out_pdf);
        page.render(out_pdf);
    }
    phantom.exit();
                // window.setTimeout(function() {
                //         if ( status === "success" ) {
                //                 page.render(out_pdf);
                //         }
                //         phantom.exit();
                //         }, 300); //超时设置
});