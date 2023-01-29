import { i18n } from './utils';

const navigationLinks = {
    start: [
        '/guide/home',
        '/guide/knowledge',
        '/guide/getting-started',
        '/guide/example',
        '/guide/run-on-desktop'
    ],
    about: [
        // '/about/changelog',
        // '/about/future',
        // '/about/contacts',
        // '/about/about'
    ]
};

export const configs = {
    dev: {
        dest: '../docs/',
        port: 8080
    },
    website: {
        base: '/DexKit/',
        icon: '/DexKit/images/logo.png',
        logo: '/images/logo.png',
        title: 'DexKit',
        locales: {
            '/en/': {
                lang: 'en-US',
                description: 'A high-performance dex runtime parsing library implemented in C++'
            },
            '/zh-cn/': {
                lang: 'zh-CN',
                description: '一个使用 C++ 实现的高性能运行时 dex 解析库'
            }
        }
    },
    github: {
        repo: 'https://github.com/LuckyPray/DexKit',
        branch: 'docs',
        dir: 'doc-source/src'
    }
};

export const navBarItems = {
    '/en/': [{
        text: 'Navigation',
        children: [{
            text: 'Get Started',
            children: [
                { text: 'Introduce', link: i18n.string(navigationLinks.start[0], 'en') },
                { text: 'Basic Knowledge', link: i18n.string(navigationLinks.start[1], 'en') },
                { text: 'Quick Start', link: i18n.string(navigationLinks.start[2], 'en') },
                { text: 'Usage Example', link: i18n.string(navigationLinks.start[3], 'en') },
            ]
        }, {
            text: 'About',
            children: [
                // { text: 'Changelog', link: i18n.string(navigationLinks.about[0], 'en') },
                // { text: 'Looking for Future', link: i18n.string(navigationLinks.about[1], 'en') },
                // { text: 'Contact Us', link: i18n.string(navigationLinks.about[2], 'en') },
                // { text: 'About this Document', link: i18n.string(navigationLinks.about[3], 'en') }
            ]
        }]
    }],
    '/zh-cn/': [{
        text: '导航',
        children: [{
            text: '入门',
            children: [
                { text: '介绍', link: i18n.string(navigationLinks.start[0], 'zh-cn') },
                { text: '基础知识', link: i18n.string(navigationLinks.start[1], 'zh-cn') },
                { text: '快速开始', link: i18n.string(navigationLinks.start[2], 'zh-cn') },
                { text: '用法示例', link: i18n.string(navigationLinks.start[3], 'zh-cn') },
            ]
        }, {
            text: '关于',
            children: [
                // { text: '更新日志', link: i18n.string(navigationLinks.about[0], 'zh-cn') },
                // { text: '展望未来', link: i18n.string(navigationLinks.about[1], 'zh-cn') },
                // { text: '联系我们', link: i18n.string(navigationLinks.about[2], 'zh-cn') },
                // { text: '关于此文档', link: i18n.string(navigationLinks.about[3], 'zh-cn') }
            ]
        }]
    }]
};

export const sideBarItems = {
    '/en/': [{
        text: 'Get Started',
        collapsible: true,
        children: i18n.array(navigationLinks.start, 'en')
    }, {
        text: 'About',
        collapsible: true,
        children: i18n.array(navigationLinks.about, 'en')
    }],
    '/zh-cn/': [{
        text: '入门',
        collapsible: true,
        children: i18n.array(navigationLinks.start, 'zh-cn')
    }, {
        text: '关于',
        collapsible: true,
        children: i18n.array(navigationLinks.about, 'zh-cn')
    }]
};
