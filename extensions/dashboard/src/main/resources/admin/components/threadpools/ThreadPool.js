import React from "react";
import {inject, observer} from "mobx-react";
import ThreadPoolDetails from "./ThreadPoolDetails";


@inject("metricsStore", "stateStore") @observer
export default class ThreadPool extends React.Component {

    componentWillMount() {
        this.props.stateStore.pageTitle = "Thread pools";
        this.props.stateStore.pageDescription = "View system thread pools";
        this.props.stateStore.headerIcon = "icon-file-alt";
    }

    render() {
        const {threadPools} = this.props.metricsStore.metrics;
        return (
            <div>
                <div class="row">
                    <div class="col-md-8">
                        <ThreadPoolDetails threadPools={threadPools}/>
                    </div>
                    <div class="col-md-4">

                    </div>
                </div>
            </div>
        )
    }
}