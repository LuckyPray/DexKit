#include <iostream>
#include <fstream>
#include <chrono>
#include <stack>
#include <map>
#include <set>
#include "dex_kit.h"

int main() {
    std::map<std::string, std::set<std::string>> obfuscate_class = {
            // value 的 set 内的多个字符串代表与运算，即同时包含填入的多个字符串才满足搜索条件
            {"C_DIALOG_UTIL",                 {"android.permission.SEND_SMS"}},
            {"C_FACADE",                      {"reSendEmo"}},
            {"C_FLASH_PIC_HELPER",            {"FlashPicHelper"}},
            {"C_BASE_PIC_DL_PROC",            {"BasePicDownl"}},
            // 如果是多个版本中，不同版本的字符串不一样，可以当成2个类分别定位，切勿当成一个类定位，否则会搜索不到
            {"C_ItemBuilderFactory",          {"ItemBuilder is: D"}},
            {"C_ItemBuilderFactory_1",        {"findItemBuilder: invoked."}},
            {"C_AIO_UTILS",                   {"openAIO by MT"}},
            {"C_ABS_GAL_SCENE",               {"gallery setColor bl"}},
            {"C_FAV_EMO_CONST",               {"http://p.qpic."}},
            {"C_FAV_EMO_CONST_1",             {"https://p.qpic."}},
            {"C_MessageRecordFactory",        {"createPicMessage"}},
            {"C_CONTACT_UTILS",               {" - WiFi"}},
            {"C_ARK_APP_ITEM_BUBBLE_BUILDER", {"debugArkMeta = "}},
            {"C_PNG_FRAME_UTIL",              {"func checkRandomPngEx"}},
            {"C_PIC_EMOTICON_INFO",           {"send emotion + 1:"}},
            {"C_SIMPLE_UI_UTIL",              {"key_simple_status_s"}},
            {"C_TROOP_GIFT_UTIL",             {".troop.send_giftTroopUtils"}},
            {"C_TROOP_GIFT_UTIL_1",           {".troop.send_giftTroopMemberUtil"}},
            {"C_QZONE_MSG_NOTIFY",            {"use small icon ,exp:"}},
            {"C_APP_CONSTANTS",               {".indivAnim/"}},
            {"C_MessageCache",                {"Q.msg.MessageCache"}},
            {"C_ScreenShotHelper",            {"onActivityResumeHideFloatView"}},
            {"C_TimeFormatterUtils",          {"TimeFormatterUtils"}},
            {"C_TogetherControlHelper",       {"SING together is click"}},
            {"C_GroupAppActivity",            {"onDrawerStartOpen"}},
            {"C_IntimateDrawer",              {"onDrawerOpened, needReqIntimateInfo: %s"}},
            {"C_ZipUtils_biz",                {",ZipEntry name: "}},
            {"C_CustomWidgetUtil",            {"^NEW$"}},
            {"C_HttpDownloader",              {"[reportHttpsResult] url="}},
            {"C_MultiMsg_Manager",            {"[sendMultiMsg]data.length = "}},
            {"C_ClockInEntryHelper",          {"isShowTogetherEntry"}},
            {"C_ClockInEntryHelper_1",        {"ClockInEntryHelper.helper"}},
            {"C_CaptureUtil",                 {"mediacodec"}},
            {"C_AvatarUtil",                  {"AvatarUtil"}},
            {"C_FaceManager",                 {"FaceManager"}},
            {"C_SmartDeviceProxyMgr",         {"SmartDeviceProxyMgr create"}},
            {"C_AIOPictureView",              {"AIOPictureView"}},
            {"C_AIOPictureView_1",            {"AIOGalleryPicView"}},
            {"C_GalleryBaseScene",            {"GalleryBaseScene"}},
            {"C_GuildHelperProvider",         {"onFoldStatus beginMoveFoldStatus:"}},
            {"C_GuildArkHelper",              {"GuildArkHelper"}},
            {"C_ReplyMsgUtils",               {"generateSourceInfo sender uin exception:"}},
            {"C_ReplyMsgSender",              {"sendReplyMessage uniseq=0"}},
            {"C_PopOutEmoticonUtil",          {"supportPopOutEmoticon isC2C="}},
            {"C_VipStatusManagerImpl",        {"getPrivilegeFlags Friends is null"}},
            {"C_SystemMessageProcessor",      {"<---handleGetFriendSystemMsgResp : decode pb filtered"}},
            {"C_OnlinePushPbPushTransMsg",    {"PbPushTransMsg muteGeneralFlag:"}},
    };
    std::map<std::string, std::set<std::string>> obfuscate_method = {
            {"N_BASE_CHAT_PIE__INIT",                           {"input set error"}},
            {"N_BASE_CHAT_PIE__INIT_1",                         {", mDefautlBtnLeft: "}},
            {"N_BASE_CHAT_PIE__handleNightMask",                {"#handleNightMask# : inNightMode"}},
            {"N_BASE_CHAT_PIE__updateSession",                  {"AIOTime updateSession end"}},
            {"N_BASE_CHAT_PIE__createMulti",                    {"createMulti"}},
            {"N_BASE_CHAT_PIE__chooseMsg",                      {"set left text from cancel"}},
            {"N_LeftSwipeReply_Helper__reply",                  {"0X800A92F"}},
            {"N_AtPanel__refreshUI",                            {"resultList = null"}},
            {"N_AtPanel__showDialogAtView",                     {"showDialogAtView"}},
            {"C_AIOPictureView",                                {"AIOPictureView"}},
            {"C_AIOPictureView_1",                              {"AIOGalleryPicView"}},
            {"N_FriendChatPie_updateUITitle",                   {"FriendChatPie updateUI_ti"}},
            {"N_ProfileCardUtil_getCard",                       {"initCard bSuperVipOpen="}},
            {"N_VasProfileTemplateController_onCardUpdate",     {"onCardUpdate fail."}},
            {"N_VasProfileTemplateController_onCardUpdate_1",   {"onCardUpdate: bgId="}},
            {"N_QQSettingMe_updateProfileBubble",               {"updateProfileBubbleMsgView"}},
            {"N_VIP_UTILS_getPrivilegeFlags",                   {"getPrivilegeFlags Friends is null"}},
            {"N_TroopChatPie_showNewTroopMemberCount",          {"showNewTroopMemberCount info is null"}},
            {"N_Conversation_onCreate",                         {"Recent_OnCreate"}},
            {"N_QQSettingMe_onResume",                          {"-->onResume!"}},
            {"N_BaseChatPie_mosaic",                            {"enableMosaicEffect"}},
            {"N_WebSecurityPluginV2_callback",                  {"check finish jr="}},
            {"N_TroopAppShortcutBarHelper_resumeAppShorcutBar", {"resumeAppShorcutBar"}},
            {"N_ChatActivityFacade_sendMsgButton",              {" sendMessage start currenttime:"}},
            {"N_FriendsStatusUtil_isChatAtTop",                 {"isChatAtTop result is: "}},
            {"N_VipUtils_getUserStatus",                        {"getUserStatus Friends is null"}},
            {"N_PhotoListPanel_resetStatus",                    {"resetStatus selectSize:"}},
            {"N_ContactUtils_getDiscussionMemberShowName",      {"getDiscussionMemberShowName uin is null"}},
            {"N_ContactUtils_getBuddyName",                     {"getBuddyName()"}},
    };

    // 使用 new 创建DexKit对象，但是得手动释放，否则会造成内存泄漏
    // 可以将指针强转为 jlong 回传给java层保存，然后后续再使用相同对象重复调用，避免重复初始化缓存
    auto dexKit = new dexkit::DexKit("../dex/qq-8.8.80.apk");

    auto now = std::chrono::system_clock::now();
    auto now_ms = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch());

    // 返回混淆map中包含所有字符串的类, 高级搜索可以使用 '^'与'$'限制字符串匹配，与正则表达式语义一致
    // result ex.
    // {"C_ABS_GAL_SCENE" -> {"Lcom/tencent/common/galleryactivity/AbstractGalleryScene;"}}
    auto res = dexKit->LocationClasses(obfuscate_class, true);
    std::cout << "---------------LocationClasses---------------\n";
    for (auto &[key, value]: res) {
        std::cout << key << " -> \n";
        for (auto &v: value) {
            std::cout << "\t" << v << "\n";
        }
    }

    // 返回混淆map中包含所有字符串的方法描述, 高级搜索可以使用 '^'与'$'限制字符串匹配，与正则表达式语义一致
    // result ex.
    // {"N_WebSecurityPluginV2_callback" -> {"Lcom/tencent/mobileqq/webview/WebSecurityPluginV2$1;->callback(Landroid/os/Bundle;)V"}}
    auto res_m = dexKit->LocationMethods(obfuscate_method, true);
    std::cout << "---------------LocationMethods---------------\n";
    for (auto &[key, value]: res_m) {
        std::cout << key << " -> \n";
        for (auto &v: value) {
            std::cout << "\t" << v << "\n";
        }
    }

    ////
    //// search qq-9.9.3.apk
    //// dex count: 30, size: 300M
    //// 运行环境: mac pro m1
    ////
    //// findClass count: 47
    //// findMethod count: 29
    //// used time: 207 ms
    ////

    std::cout << "findClass count: " << res.size() << std::endl;
    std::cout << "findMethod count: " << res_m.size() << std::endl;

    auto now1 = std::chrono::system_clock::now();
    auto now_ms1 = std::chrono::duration_cast<std::chrono::milliseconds>(now1.time_since_epoch());
    std::cout << "used time: " << now_ms1.count() - now_ms.count() << " ms\n";

    // 释放资源
    delete dexKit;

    return 0;
}